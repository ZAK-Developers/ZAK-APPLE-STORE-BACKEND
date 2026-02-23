package com.zakapplestore.ZAKAppleStore.config;

import com.zakapplestore.ZAKAppleStore.entity.Role;
import com.zakapplestore.ZAKAppleStore.entity.User;
import com.zakapplestore.ZAKAppleStore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Value("${bootstrap.admin.email:admin@zakapplestore.com}")
    private String adminEmail;

    @Value("${bootstrap.admin.password:admin123}")
    private String adminPassword;

    @Value("${bootstrap.admin.full-name:Store Admin}")
    private String adminFullName;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .fullName(adminFullName)
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            System.out.println("Default admin user created.");
        }
    }
}
