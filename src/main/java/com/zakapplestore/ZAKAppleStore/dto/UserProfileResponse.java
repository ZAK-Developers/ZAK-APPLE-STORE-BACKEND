package com.zakapplestore.ZAKAppleStore.dto;

import com.zakapplestore.ZAKAppleStore.entity.AuthProvider;
import com.zakapplestore.ZAKAppleStore.entity.UserRole;
import com.zakapplestore.ZAKAppleStore.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID id;
    private String username;
    private String email;
    private String mobile;
    private String profileImage;
    private boolean emailVerified;
    private boolean mobileVerified;
    private UserRole role;
    private UserStatus status;
    private AuthProvider provider;
    private List<AddressResponse> addresses;
}
