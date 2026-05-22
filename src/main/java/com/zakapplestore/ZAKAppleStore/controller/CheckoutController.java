package com.zakapplestore.ZAKAppleStore.controller;

import com.zakapplestore.ZAKAppleStore.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(Authentication authentication, @RequestBody Map<String, String> request) {
        try {
            String email = authentication.getName();
            String requestId = request.getOrDefault("requestId", java.util.UUID.randomUUID().toString());
            
            Map<String, Object> response = checkoutService.initiateCheckout(email, requestId);
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
}
