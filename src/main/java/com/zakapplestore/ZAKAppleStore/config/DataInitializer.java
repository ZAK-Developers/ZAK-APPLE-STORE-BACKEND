package com.zakapplestore.ZAKAppleStore.config;

import com.zakapplestore.ZAKAppleStore.entity.AuthProvider;
import com.zakapplestore.ZAKAppleStore.entity.User;
import com.zakapplestore.ZAKAppleStore.entity.UserRole;
import com.zakapplestore.ZAKAppleStore.entity.UserStatus;
import com.zakapplestore.ZAKAppleStore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${bootstrap.admin.email:admin@zakapplestore.com}")
    private String adminEmail;

    @Value("${bootstrap.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${bootstrap.admin.username:admin}")
    private String adminUsername;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmailIgnoreCase(adminEmail).isEmpty()) {
            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail.toLowerCase())
                    .password(passwordEncoder.encode(adminPassword))
                    .provider(AuthProvider.LOCAL)
                    .emailVerified(true)
                    .mobileVerified(false)
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(admin);
        }
    }
}
