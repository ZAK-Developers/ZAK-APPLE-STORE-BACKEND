package com.zakapplestore.ZAKAppleStore.controller;

import com.zakapplestore.ZAKAppleStore.dto.CheckoutCreateOrderRequest;
import com.zakapplestore.ZAKAppleStore.dto.OrderFailureRequest;
import com.zakapplestore.ZAKAppleStore.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(Authentication authentication, @RequestBody CheckoutCreateOrderRequest request) {
        try {
            String email = authentication.getName();
            String requestId = request.getRequestId() != null && !request.getRequestId().isBlank()
                    ? request.getRequestId()
                    : java.util.UUID.randomUUID().toString();
            
            Map<String, Object> response = checkoutService.initiateCheckout(email, requestId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<Map<String, String>> verifyPayment(Authentication authentication, @RequestBody Map<String, Object> payload) {
        try {
            String email = authentication.getName();
            Long orderId = Long.valueOf(payload.get("orderId").toString());
            String razorpayPaymentId = (String) payload.get("razorpayPaymentId");
            String razorpayOrderId = (String) payload.get("razorpayOrderId");
            String razorpaySignature = (String) payload.get("razorpaySignature");

            checkoutService.verifyPaymentAndCompleteOrder(email, orderId, razorpayPaymentId, razorpayOrderId, razorpaySignature);
            
            return ResponseEntity.ok(Map.of("status", "success", "message", "Payment verified successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    @PostMapping("/mark-failed/{orderId}")
    public ResponseEntity<Map<String, String>> markOrderFailed(
            Authentication authentication,
            @PathVariable Long orderId,
            @RequestBody(required = false) OrderFailureRequest request
    ) {
        try {
            checkoutService.markOrderFailed(
                    authentication.getName(),
                    orderId,
                    request != null ? request.getReason() : null
            );
            return ResponseEntity.ok(Map.of("status", "failed_marked"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }
}
