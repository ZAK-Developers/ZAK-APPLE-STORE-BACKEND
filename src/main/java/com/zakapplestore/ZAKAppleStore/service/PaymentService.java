package com.zakapplestore.ZAKAppleStore.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
public class PaymentService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    private RazorpayClient razorpayClient;

    @PostConstruct
    public void init() throws RazorpayException {
        this.razorpayClient = new RazorpayClient(keyId, keySecret);
    }

    public Order createPaymentOrder(String receiptId, BigDecimal amountInINR) throws RazorpayException {
        JSONObject options = new JSONObject();
        // Razorpay expects amount in paisa (amount * 100)
        long amountInPaisa = amountInINR.multiply(new BigDecimal("100")).longValue();
        options.put("amount", amountInPaisa);
        options.put("currency", "INR");
        options.put("receipt", receiptId);
        
        return razorpayClient.orders.create(options);
    }

    public boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            return Utils.verifyPaymentSignature(options, keySecret);
        } catch (RazorpayException e) {
            log.error("Error verifying payment signature", e);
            return false;
        }
    }

    public String getKeyId() {
        return keyId;
    }
}
