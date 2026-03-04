package com.zakapplestore.ZAKAppleStore.controller;

import com.zakapplestore.ZAKAppleStore.dto.AuthResponse;
import com.zakapplestore.ZAKAppleStore.dto.ForgotPasswordRequest;
import com.zakapplestore.ZAKAppleStore.dto.LoginRequest;
import com.zakapplestore.ZAKAppleStore.dto.MessageResponse;
import com.zakapplestore.ZAKAppleStore.dto.OtpResponse;
import com.zakapplestore.ZAKAppleStore.dto.RegisterRequest;
import com.zakapplestore.ZAKAppleStore.dto.RegisterResponse;
import com.zakapplestore.ZAKAppleStore.dto.ResetPasswordRequest;
import com.zakapplestore.ZAKAppleStore.dto.SendEmailOtpRequest;
import com.zakapplestore.ZAKAppleStore.dto.SendMobileOtpRequest;
import com.zakapplestore.ZAKAppleStore.dto.VerifyEmailOtpRequest;
import com.zakapplestore.ZAKAppleStore.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/send-email-otp")
    public ResponseEntity<MessageResponse> sendEmailOtp(@Valid @RequestBody SendEmailOtpRequest request) {
        return ResponseEntity.ok(authService.sendEmailOtp(request));
    }

    @PostMapping("/verify-email-otp")
    public ResponseEntity<MessageResponse> verifyEmailOtp(@Valid @RequestBody VerifyEmailOtpRequest request) {
        return ResponseEntity.ok(authService.verifyEmailOtp(request));
    }

    @PostMapping("/send-mobile-otp")
    public ResponseEntity<OtpResponse> sendMobileOtp(@Valid @RequestBody SendMobileOtpRequest request) {
        return ResponseEntity.ok(authService.sendMobileOtp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @GetMapping("/google")
    public ResponseEntity<Map<String, String>> getGoogleLoginUrl() {
        return ResponseEntity.ok(Map.of("url", "/oauth2/authorization/google"));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        authService.logout(authHeader);
        return ResponseEntity.ok(MessageResponse.builder().message("Logged out successfully").build());
    }
}
