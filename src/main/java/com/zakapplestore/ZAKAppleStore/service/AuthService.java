package com.zakapplestore.ZAKAppleStore.service;

import com.zakapplestore.ZAKAppleStore.dto.AuthResponse;
import com.zakapplestore.ZAKAppleStore.dto.LoginRequest;
import com.zakapplestore.ZAKAppleStore.dto.RegisterRequest;
import com.zakapplestore.ZAKAppleStore.entity.Role;
import com.zakapplestore.ZAKAppleStore.entity.User;
import com.zakapplestore.ZAKAppleStore.repository.UserRepository;
import com.zakapplestore.ZAKAppleStore.security.TokenBlacklistService;
import com.zakapplestore.ZAKAppleStore.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .mobile(request.getMobile())
                .role(request.getRole() != null ? request.getRole() : Role.CUSTOMER)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .userId(user.getId())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .userId(user.getId())
                .build();
    }

    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authHeader.substring(7);
        Date expiration = jwtUtil.extractExpiration(token);
        tokenBlacklistService.blacklistToken(token, expiration);
    }
}
