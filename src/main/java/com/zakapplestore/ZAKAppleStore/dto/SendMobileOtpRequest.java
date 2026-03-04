package com.zakapplestore.ZAKAppleStore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMobileOtpRequest {

    @NotBlank(message = "Mobile is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Mobile must contain 10 to 15 digits")
    private String mobile;
}
