package com.zakapplestore.ZAKAppleStore.service;

import com.zakapplestore.ZAKAppleStore.entity.OtpType;
import com.zakapplestore.ZAKAppleStore.entity.OtpVerification;
import com.zakapplestore.ZAKAppleStore.exception.BadRequestException;
import com.zakapplestore.ZAKAppleStore.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final String DUMMY_MOBILE_OTP = "123456";

    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;

    @Value("${app.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Transactional
    public String sendEmailOtp(String email, OtpType type) {
        String otp = generateOtp();
        saveOtp(email, null, otp, type);
        emailService.sendOtpMail(email, otp, type);
        return otp;
    }

    @Transactional
    public String sendDummyMobileOtp(String mobile) {
        saveOtp(null, mobile, DUMMY_MOBILE_OTP, OtpType.MOBILE_VERIFICATION);
        return DUMMY_MOBILE_OTP;
    }

    @Transactional
    public void verifyEmailOtp(String email, String otp, OtpType type) {
        OtpVerification otpVerification = otpVerificationRepository
                .findTopByEmailAndOtpAndTypeAndVerifiedFalseOrderByCreatedAtDesc(email, otp, type)
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));

        if (otpVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired. Please request a new OTP.");
        }

        otpVerification.setVerified(true);
        otpVerificationRepository.save(otpVerification);
    }

    @Transactional(readOnly = true)
    public void ensureEmailOtpVerified(String email, OtpType type) {
        OtpVerification otpVerification = otpVerificationRepository
                .findTopByEmailAndTypeAndVerifiedTrueOrderByCreatedAtDesc(email, type)
                .orElseThrow(() -> new BadRequestException("Please verify your email OTP before registering."));

        if (otpVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Email OTP verification has expired. Please request a new OTP.");
        }
    }

    private void saveOtp(String email, String mobile, String otp, OtpType type) {
        OtpVerification otpVerification = OtpVerification.builder()
                .email(email)
                .mobile(mobile)
                .otp(otp)
                .type(type)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .verified(false)
                .build();
        otpVerificationRepository.save(otpVerification);
    }

    private String generateOtp() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
