package com.zakapplestore.ZAKAppleStore.controller;

import com.zakapplestore.ZAKAppleStore.dto.AddressRequest;
import com.zakapplestore.ZAKAppleStore.dto.AddressResponse;
import com.zakapplestore.ZAKAppleStore.dto.AdminCustomerResponse;
import com.zakapplestore.ZAKAppleStore.dto.LoginHistoryResponse;
import com.zakapplestore.ZAKAppleStore.dto.MessageResponse;
import com.zakapplestore.ZAKAppleStore.dto.UpdateProfileRequest;
import com.zakapplestore.ZAKAppleStore.dto.UserProfileResponse;
import com.zakapplestore.ZAKAppleStore.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getProfile(authentication.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(authentication.getName(), request));
    }

    @PostMapping("/address")
    public ResponseEntity<AddressResponse> addAddress(
            Authentication authentication,
            @Valid @RequestBody AddressRequest request
    ) {
        return ResponseEntity.ok(userService.addAddress(authentication.getName(), request));
    }

    @GetMapping("/address")
    public ResponseEntity<List<AddressResponse>> getAddresses(Authentication authentication) {
        return ResponseEntity.ok(userService.getAddresses(authentication.getName()));
    }

    @DeleteMapping("/address/{id}")
    public ResponseEntity<MessageResponse> deleteAddress(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        userService.deleteAddress(authentication.getName(), id);
        return ResponseEntity.ok(MessageResponse.builder().message("Address deleted successfully").build());
    }

    @GetMapping("/login-history")
    public ResponseEntity<List<LoginHistoryResponse>> getLoginHistory(Authentication authentication) {
        return ResponseEntity.ok(userService.getLoginHistory(authentication.getName()));
    }

    @GetMapping("/admin/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminCustomerResponse>> getRegisteredCustomers() {
        return ResponseEntity.ok(userService.getRegisteredCustomers());
    }
}
