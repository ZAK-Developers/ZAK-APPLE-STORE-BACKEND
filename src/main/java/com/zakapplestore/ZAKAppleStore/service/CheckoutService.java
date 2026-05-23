package com.zakapplestore.ZAKAppleStore.service;

import com.zakapplestore.ZAKAppleStore.dto.CartResponse;
import com.zakapplestore.ZAKAppleStore.dto.CheckoutAddressRequest;
import com.zakapplestore.ZAKAppleStore.dto.CheckoutCreateOrderRequest;
import com.razorpay.RazorpayException;
import com.zakapplestore.ZAKAppleStore.dto.CartItemResponse;
import com.zakapplestore.ZAKAppleStore.entity.Order;
import com.zakapplestore.ZAKAppleStore.entity.OrderItem;
import com.zakapplestore.ZAKAppleStore.entity.OrderStatus;
import com.zakapplestore.ZAKAppleStore.entity.Payment;
import com.zakapplestore.ZAKAppleStore.entity.User;
import com.zakapplestore.ZAKAppleStore.exception.BadRequestException;
import com.zakapplestore.ZAKAppleStore.exception.ResourceNotFoundException;
import com.zakapplestore.ZAKAppleStore.repository.CartRepository;
import com.zakapplestore.ZAKAppleStore.repository.OrderItemRepository;
import com.zakapplestore.ZAKAppleStore.repository.OrderRepository;
import com.zakapplestore.ZAKAppleStore.repository.PaymentRepository;
import com.zakapplestore.ZAKAppleStore.repository.ProductRepository;
import com.zakapplestore.ZAKAppleStore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartService cartService;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final com.zakapplestore.ZAKAppleStore.messaging.OrderEventPublisher orderEventPublisher;

    @Transactional
    public Map<String, Object> initiateCheckout(String email, String requestId, CheckoutCreateOrderRequest request) throws RazorpayException {
        // 1. Idempotency Check
        if (orderRepository.existsByRequestId(requestId)) {
            throw new BadRequestException("Order request already processed.");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Strict total calculation and validation
        CartResponse cart = cartService.getCart(email);
        List<CartItemResponse> checkoutItems = filterCheckoutItems(cart, request);
        if (checkoutItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        BigDecimal subtotal = checkoutItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal grandTotal = subtotal.add(tax);
        CheckoutAddressRequest shippingAddress = request.getShippingAddress();
        if (shippingAddress == null || isBlank(shippingAddress.getAddressLine1()) || isBlank(shippingAddress.getFullName())) {
            throw new BadRequestException("Shipping address is required");
        }

        // 3. Create initial order
        Order order = Order.builder()
                .orderNumber("ORD-" + System.currentTimeMillis())
                .requestId(requestId)
                .user(user)
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .shippingFullName(shippingAddress.getFullName())
                .shippingPhone(shippingAddress.getPhone())
                .shippingAddressLine1(shippingAddress.getAddressLine1())
                .shippingAddressLine2(shippingAddress.getAddressLine2())
                .shippingCity(shippingAddress.getCity())
                .shippingState(shippingAddress.getState())
                .shippingPinCode(shippingAddress.getPinCode())
                .shippingCountry(shippingAddress.getCountry())
                .subtotal(subtotal)
                .tax(tax)
                .shippingFee(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .grandTotal(grandTotal)
                .build();
        
        order = orderRepository.save(order);

        // Save snapshot of order items
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItemResponse cartItem : checkoutItems) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(cartItem.getProductId())
                    .productName(cartItem.getProductName())
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .build();
            orderItems.add(orderItem);
        }
        orderItemRepository.saveAll(orderItems);

        // 4. Create payment gateway order
        com.razorpay.Order razorpayOrder = paymentService.createPaymentOrder(order.getOrderNumber(), grandTotal);

        Payment payment = Payment.builder()
                .order(order)
                .gatewayOrderId(razorpayOrder.get("id"))
                .status("CREATED")
                .build();
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.PAYMENT_PENDING);
        orderRepository.save(order);

        return Map.of(
            "orderId", order.getId(),
            "razorpayOrderId", razorpayOrder.get("id"),
            "amount", razorpayOrder.get("amount"),
            "currency", razorpayOrder.get("currency"),
            "keyId", paymentService.getKeyId()
        );
    }

    @Transactional
    public void verifyPaymentAndCompleteOrder(String email, Long orderId, String razorpayPaymentId, String razorpayOrderId, String razorpaySignature) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new BadRequestException("Order does not belong to user");
        }

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.PROCESSING) {
            return; // Already processed
        }

        // 1. Verify Signature
        boolean isValid = paymentService.verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);
        if (!isValid) {
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            throw new BadRequestException("Payment signature verification failed");
        }

        // 2. Lock Inventory and Reduce Stock
        for (OrderItem item : order.getItems()) {
            // Using pessimistic lock to prevent concurrent overselling during checkout
            var productResponse = productRepository.findByIdWithPessimisticLock(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            if (productResponse.getStockQuantity() < item.getQuantity()) {
                // In a real scenario, initiate refund here
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);
                throw new BadRequestException("Not enough stock for product: " + productResponse.getProductName());
            }

            // Reduce stock
            // Since productRepository uses JDBC, we execute an update query
            productRepository.updateStock(item.getProductId(), productResponse.getStockQuantity() - item.getQuantity());
        }

        // 3. Update payment and order status
        Payment payment = paymentRepository.findByGatewayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found"));
        payment.setGatewayPaymentId(razorpayPaymentId);
        payment.setSignature(razorpaySignature);
        payment.setStatus("SUCCESS");
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        // 4. Clear only purchased items from cart
        cartRepository.clearSelectedItems(
                order.getUser().getId(),
                order.getItems().stream().map(OrderItem::getProductId).toList()
        );

        // 5. Publish Event (To be handled by RabbitMQ)
        orderEventPublisher.publishOrderSuccess(order.getId(), order.getUser().getEmail(), order.getOrderNumber());
    }

    @Transactional
    public void markOrderFailed(String email, Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new BadRequestException("Order does not belong to user");
        }

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.PROCESSING) {
            throw new BadRequestException("Paid orders cannot be marked as failed");
        }

        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);

        if (order.getPayment() != null) {
            Payment payment = order.getPayment();
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
        }
    }

    private List<CartItemResponse> filterCheckoutItems(CartResponse cart, CheckoutCreateOrderRequest request) {
        if (cart.getItems() == null) {
            return List.of();
        }

        if (request.getProductId() == null) {
            return cart.getItems();
        }

        return cart.getItems().stream()
                .filter(item -> Objects.equals(item.getProductId(), request.getProductId()))
                .filter(item -> isBlank(request.getColor()) || Objects.equals(item.getColor(), request.getColor()))
                .filter(item -> isBlank(request.getStorage()) || Objects.equals(item.getStorage(), request.getStorage()))
                .toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
