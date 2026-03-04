package com.zakapplestore.ZAKAppleStore.dto;

import com.zakapplestore.ZAKAppleStore.entity.AuthProvider;
import com.zakapplestore.ZAKAppleStore.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCustomerResponse {

    private UUID id;
    private String username;
    private String email;
    private String mobile;
    private AuthProvider provider;
    private boolean emailVerified;
    private boolean mobileVerified;
    private UserStatus status;
    private LocalDateTime createdAt;
}
