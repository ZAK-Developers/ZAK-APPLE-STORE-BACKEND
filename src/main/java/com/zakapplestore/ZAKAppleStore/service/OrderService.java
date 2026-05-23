package com.zakapplestore.ZAKAppleStore.service;

import com.zakapplestore.ZAKAppleStore.dto.OrderItemSummaryResponse;
import com.zakapplestore.ZAKAppleStore.dto.OrderSummaryResponse;
import com.zakapplestore.ZAKAppleStore.entity.Order;
import com.zakapplestore.ZAKAppleStore.entity.Payment;
import com.zakapplestore.ZAKAppleStore.entity.User;
import com.zakapplestore.ZAKAppleStore.exception.ResourceNotFoundException;
import com.zakapplestore.ZAKAppleStore.repository.OrderRepository;
import com.zakapplestore.ZAKAppleStore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getOrdersForUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return orderRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getAllOrdersForAdmin() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private OrderSummaryResponse toResponse(Order order) {
        Payment payment = order.getPayment();
        return OrderSummaryResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getShippingFullName() != null && !order.getShippingFullName().isBlank()
                        ? order.getShippingFullName()
                        : order.getUser().getUsername())
                .email(order.getUser().getEmail())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(payment != null ? payment.getStatus() : "NOT_STARTED")
                .orderStatus(order.getStatus().name())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .grandTotal(order.getGrandTotal())
                .createdAt(order.getCreatedAt())
                .shippingAddress(buildShippingAddress(order))
                .items(order.getItems().stream()
                        .map(item -> OrderItemSummaryResponse.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private String buildShippingAddress(Order order) {
        return String.join(", ",
                notBlank(order.getShippingAddressLine1()),
                notBlank(order.getShippingAddressLine2()),
                notBlank(order.getShippingCity()),
                notBlank(order.getShippingState()),
                notBlank(order.getShippingPinCode()),
                notBlank(order.getShippingCountry())
        ).replaceAll("(, )+", ", ").replaceAll("^, |, $", "");
    }

    private String notBlank(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
