package com.zakapplestore.ZAKAppleStore.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutAddressRequest {
    private String fullName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pinCode;
    private String country;
}
