package com.zakapplestore.ZAKAppleStore.repository;

import com.zakapplestore.ZAKAppleStore.entity.OtpType;
import com.zakapplestore.ZAKAppleStore.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {

    Optional<OtpVerification> findTopByEmailAndOtpAndTypeAndVerifiedFalseOrderByCreatedAtDesc(
            String email,
            String otp,
            OtpType type
    );

    Optional<OtpVerification> findTopByEmailAndTypeAndVerifiedFalseOrderByCreatedAtDesc(
            String email,
            OtpType type
    );

    Optional<OtpVerification> findTopByEmailAndTypeAndVerifiedTrueOrderByCreatedAtDesc(
            String email,
            OtpType type
    );

    Optional<OtpVerification> findTopByMobileAndOtpAndTypeAndVerifiedFalseOrderByCreatedAtDesc(
            String mobile,
            String otp,
            OtpType type
    );

    List<OtpVerification> findByExpiresAtBefore(LocalDateTime time);
}
