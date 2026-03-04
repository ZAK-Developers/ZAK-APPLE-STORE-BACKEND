package com.zakapplestore.ZAKAppleStore.service;

import com.zakapplestore.ZAKAppleStore.entity.OtpType;
import com.zakapplestore.ZAKAppleStore.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final String MAIL_NOT_CONFIGURED_MESSAGE =
            "SMTP sender email is not configured. Please set spring.mail.username.";
    private static final String MAIL_AUTH_FAILED_MESSAGE =
            "SMTP authentication failed. Verify spring.mail.username and Gmail App Password (use 16-character app password without spaces).";

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    @PostConstruct
    void normalizeMailPassword() {
        if (!(javaMailSender instanceof JavaMailSenderImpl sender)) {
            return;
        }

        String password = sender.getPassword();
        if (!StringUtils.hasText(password)) {
            return;
        }

        String normalizedPassword = password.replace(" ", "");
        if (!password.equals(normalizedPassword)) {
            sender.setPassword(normalizedPassword);
            log.info("Removed whitespace from SMTP password configuration.");
        }
    }

    public void sendOtpMail(String toEmail, String otp, OtpType otpType) {
        String subject;
        String purpose;

        if (otpType == OtpType.PASSWORD_RESET) {
            subject = "Password Reset OTP - ZAK Apple Store";
            purpose = "reset your password";
        } else if (otpType == OtpType.MOBILE_VERIFICATION) {
            subject = "Mobile Verification OTP - ZAK Apple Store";
            purpose = "verify your mobile";
        } else {
            subject = "Email Verification OTP - ZAK Apple Store";
            purpose = "verify your email";
        }

        String body = """
                Hello,

                Your OTP to %s is: %s

                This OTP is valid for 10 minutes.
                If you did not request this, please ignore this email.

                Regards,
                ZAK Apple Store Team
                """.formatted(purpose, otp);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveSenderEmail());
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            javaMailSender.send(message);
        } catch (MailAuthenticationException ex) {
            log.error("SMTP authentication failed while sending OTP to {}", toEmail, ex);
            throw new BadRequestException(MAIL_AUTH_FAILED_MESSAGE);
        } catch (MailSendException ex) {
            log.error("SMTP send failure for recipient {}", toEmail, ex);
            throw new BadRequestException("Unable to deliver OTP email to the recipient. Please verify the email address.");
        } catch (MailException ex) {
            log.error("Unexpected mail error while sending OTP to {}", toEmail, ex);
            throw new BadRequestException("Unable to send OTP email at the moment. Please try again.");
        }
    }

    private String resolveSenderEmail() {
        if (!StringUtils.hasText(senderEmail) || "your-email".equalsIgnoreCase(senderEmail.trim())) {
            throw new BadRequestException(MAIL_NOT_CONFIGURED_MESSAGE);
        }
        return senderEmail.trim();
    }
}
