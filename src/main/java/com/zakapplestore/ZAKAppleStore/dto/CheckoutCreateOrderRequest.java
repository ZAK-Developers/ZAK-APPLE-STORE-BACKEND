package com.zakapplestore.ZAKAppleStore.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CheckoutCreateOrderRequest {
    private String requestId;
    private String paymentMethod;
    private UUID productId;
    private String color;
    private String storage;
    private CheckoutAddressRequest shippingAddress;
}
