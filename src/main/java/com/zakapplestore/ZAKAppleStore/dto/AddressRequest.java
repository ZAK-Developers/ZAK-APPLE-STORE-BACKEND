package com.zakapplestore.ZAKAppleStore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 120, message = "Full name is too long")
    private String fullName;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone must contain 10 to 15 digits")
    private String phone;

    @NotBlank(message = "Address line 1 is required")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City is too long")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State is too long")
    private String state;

    @NotBlank(message = "Pin code is required")
    @Pattern(regexp = "^[0-9A-Za-z\\- ]{4,10}$", message = "Invalid pin code")
    private String pinCode;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country is too long")
    private String country;

    private Boolean isDefault;
}
