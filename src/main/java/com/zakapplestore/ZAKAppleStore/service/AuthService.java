package com.zakapplestore.ZAKAppleStore.service;

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
import com.zakapplestore.ZAKAppleStore.dto.UserSummaryResponse;
import com.zakapplestore.ZAKAppleStore.dto.VerifyEmailOtpRequest;
import com.zakapplestore.ZAKAppleStore.entity.AuthProvider;
import com.zakapplestore.ZAKAppleStore.entity.LoginStatus;
import com.zakapplestore.ZAKAppleStore.entity.OtpType;
import com.zakapplestore.ZAKAppleStore.entity.User;
import com.zakapplestore.ZAKAppleStore.entity.UserRole;
import com.zakapplestore.ZAKAppleStore.entity.UserStatus;
import com.zakapplestore.ZAKAppleStore.exception.BadRequestException;
import com.zakapplestore.ZAKAppleStore.exception.EmailNotRegisteredException;
import com.zakapplestore.ZAKAppleStore.exception.InvalidPasswordException;
import com.zakapplestore.ZAKAppleStore.exception.UnauthorizedException;
import com.zakapplestore.ZAKAppleStore.repository.UserRepository;
import com.zakapplestore.ZAKAppleStore.security.JwtUtil;
import com.zakapplestore.ZAKAppleStore.security.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String NOT_REGISTERED_MESSAGE = "This email is not registered. Please register first.";
    private static final String LOGIN_EMAIL_NOT_REGISTERED_MESSAGE = "Email is not registered. Please sign up before logging in.";
    private static final String LOGIN_INVALID_PASSWORD_MESSAGE = "Invalid password. Please try again.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final OtpService otpService;
    private final LoginHistoryService loginHistoryService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email already exists");
        }

        otpService.ensureEmailOtpVerified(email, OtpType.EMAIL_VERIFICATION);

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(email)
                .mobile(request.getMobile())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider(AuthProvider.LOCAL)
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .mobileVerified(false)
                .build();

        user = userRepository.save(user);

        return RegisterResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .message("Registration successful")
                .build();
    }

    @Transactional
    public MessageResponse sendEmailOtp(SendEmailOtpRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email already registered. Please login.");
        }

        otpService.sendEmailOtp(email, OtpType.EMAIL_VERIFICATION);

        return MessageResponse.builder()
                .message("OTP sent successfully to email")
                .build();
    }

    @Transactional
    public MessageResponse verifyEmailOtp(VerifyEmailOtpRequest request) {
        String email = normalizeEmail(request.getEmail());
        otpService.verifyEmailOtp(email, request.getOtp(), OtpType.EMAIL_VERIFICATION);

        return MessageResponse.builder()
                .message("Email verified successfully")
                .build();
    }

    @Transactional
    public OtpResponse sendMobileOtp(SendMobileOtpRequest request) {
        String otp = otpService.sendDummyMobileOtp(request.getMobile());
        return OtpResponse.builder()
                .message("Dummy OTP generated and stored successfully")
                .otp(otp)
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new EmailNotRegisteredException(LOGIN_EMAIL_NOT_REGISTERED_MESSAGE));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User account is not active");
        }

        if (user.getProvider() == AuthProvider.GOOGLE && (user.getPassword() == null || user.getPassword().isBlank())) {
            throw new BadRequestException("This account is registered with Google. Please login with Google.");
        }

        if (user.getPassword() == null || user.getPassword().isBlank()
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginHistoryService.recordLogin(user, httpRequest, LoginStatus.FAILED);
            throw new InvalidPasswordException(LOGIN_INVALID_PASSWORD_MESSAGE);
        }

        String token = jwtUtil.generateToken(user);
        loginHistoryService.recordLogin(user, httpRequest, LoginStatus.SUCCESS);
        return AuthResponse.builder()
                .success(true)
                .token(token)
                .user(mapToUserSummary(user))
                .build();
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException(NOT_REGISTERED_MESSAGE));

        if (user.getProvider() == AuthProvider.GOOGLE && (user.getPassword() == null || user.getPassword().isBlank())) {
            throw new BadRequestException("Google account password cannot be reset using email OTP.");
        }

        otpService.sendEmailOtp(email, OtpType.PASSWORD_RESET);
        return MessageResponse.builder()
                .message("Password reset OTP sent to your email")
                .build();
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException(NOT_REGISTERED_MESSAGE));

        otpService.verifyEmailOtp(email, request.getOtp(), OtpType.PASSWORD_RESET);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setProvider(AuthProvider.LOCAL);
        userRepository.save(user);

        return MessageResponse.builder()
                .message("Password reset successful")
                .build();
    }

    @Transactional
    public String processGoogleLogin(OAuth2User oAuth2User, HttpServletRequest request) {
        String email = normalizeEmail((String) oAuth2User.getAttributes().get("email"));
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Google account email not available");
        }

        String googleId = String.valueOf(oAuth2User.getAttributes().get("sub"));
        String name = (String) oAuth2User.getAttributes().getOrDefault("name", email.split("@")[0]);
        String picture = (String) oAuth2User.getAttributes().get("picture");

        User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() ->
                User.builder()
                        .username(generateUsername(name, email))
                        .email(email)
                        .provider(AuthProvider.GOOGLE)
                        .providerId(googleId)
                        .emailVerified(true)
                        .mobileVerified(false)
                        .role(UserRole.CUSTOMER)
                        .status(UserStatus.ACTIVE)
                        .profileImage(picture)
                        .build()
        );

        user.setProviderId(googleId);
        user.setProvider(AuthProvider.GOOGLE);
        user.setEmailVerified(true);
        if (picture != null && !picture.isBlank()) {
            user.setProfileImage(picture);
        }

        user = userRepository.save(user);
        loginHistoryService.recordLogin(user, request, LoginStatus.SUCCESS);
        return jwtUtil.generateToken(user);
    }

    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authHeader.substring(7);
        Date expiration = jwtUtil.extractExpiration(token);
        tokenBlacklistService.blacklistToken(token, expiration);
    }

    private UserSummaryResponse mapToUserSummary(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .profileImage(user.getProfileImage())
                .emailVerified(user.isEmailVerified())
                .mobileVerified(user.isMobileVerified())
                .role(user.getRole())
                .provider(user.getProvider())
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String generateUsername(String name, String email) {
        if (name != null && !name.isBlank()) {
            return name.trim().replaceAll("\\s+", "_");
        }
        return email.split("@")[0];
    }
}
