package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.entity.Otp;
import com.railswad.deliveryservice.entity.User;
import com.railswad.deliveryservice.repository.OtpRepository;
import com.railswad.deliveryservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Optional;

@Service
public class OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private final OtpRepository otpRepository;
    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;
    private static final int OTP_VALIDITY_MINUTES = 3;
    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final int OTP_LENGTH = 6;
    private static final int RATE_LIMIT_MINUTES = 1;
    private static final String OTP_TYPE = "REGISTRATION";

    public OtpService(OtpRepository otpRepository, UserRepository userRepository, JavaMailSender javaMailSender) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.javaMailSender = javaMailSender;
    }

    @Transactional
    public void generateAndSendOtp(Long userId, String ipAddress, String deviceInfo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Check rate limiting
        Optional<Otp> recentOtp = otpRepository.findByUserIdAndOtpType(userId, OTP_TYPE);
        if (recentOtp.isPresent() && recentOtp.get().getCreatedAt().isAfter(ZonedDateTime.now().minusMinutes(RATE_LIMIT_MINUTES))) {
            throw new RuntimeException("Please wait before requesting a new OTP");
        }

        try {
            String otp = generateSecureOtp();

            // Check for existing OTP
            Otp otpEntity;
            if (recentOtp.isPresent()) {
                otpEntity = recentOtp.get();
                otpEntity.setOtpCode(otp);
                otpEntity.setCreatedAt(ZonedDateTime.now());
                otpEntity.setExpirationTime(ZonedDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
                otpEntity.setUsed(false);
                otpEntity.setAttemptCount(0);
                otpEntity.setIpAddress(ipAddress);
                otpEntity.setDeviceInfo(deviceInfo);
            } else {
                otpEntity = new Otp();
                otpEntity.setUserId(userId);
                otpEntity.setOtpCode(otp);
                otpEntity.setOtpType(OTP_TYPE);
                otpEntity.setCreatedAt(ZonedDateTime.now());
                otpEntity.setExpirationTime(ZonedDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
                otpEntity.setUsed(false);
                otpEntity.setAttemptCount(0);
                otpEntity.setIpAddress(ipAddress);
                otpEntity.setDeviceInfo(deviceInfo);
            }

            // Save OTP
            otpRepository.saveAndFlush(otpEntity);

            // Send OTP via email
            sendOtp(user.getEmail(), otp);
            logger.info("OTP: {}", otp);
            logger.info("OTP generated and sent for user ID: {}", userId);
        } catch (MailException e) {
            logger.error("Failed to send OTP email to user ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to send OTP email", e);
        } catch (Exception e) {
            logger.error("Error generating OTP for user ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to generate OTP", e);
        }
    }

    @Transactional
    public boolean verifyOtp(Long userId, String otpCode, String ipAddress) {
        Optional<Otp> otpEntity = otpRepository.findByUserIdAndOtpCodeAndOtpType(userId, otpCode, OTP_TYPE);
        if (otpEntity.isEmpty()) {
            logger.warn("Invalid OTP attempt for user ID: {}", userId);
            return false;
        }

        Otp foundOtp = otpEntity.get();
        if (foundOtp.isUsed()) {
            logger.warn("OTP already used for user ID: {}", userId);
            return false;
        }

        if (ZonedDateTime.now().isAfter(foundOtp.getExpirationTime())) {
            logger.warn("Expired OTP attempt for user ID: {}", userId);
            return false;
        }

        int attemptCount = foundOtp.getAttemptCount() + 1;
        foundOtp.setAttemptCount(attemptCount);

        if (attemptCount >= MAX_OTP_ATTEMPTS) {
            logger.warn("Max OTP attempts exceeded for user ID: {}", userId);
            otpRepository.deleteByUserIdAndOtpType(userId, OTP_TYPE);
            return false;
        }

        // Update IP for tracking
        foundOtp.setIpAddress(ipAddress);
        otpRepository.save(foundOtp);

        if (!foundOtp.getOtpCode().equals(otpCode)) {
            logger.warn("Incorrect OTP for user ID: {}", userId);
            return false;
        }

        // Mark OTP as used and clean up
        foundOtp.setUsed(true);
        otpRepository.save(foundOtp);
        logger.info("OTP verified successfully for user ID: {}", userId);
        return true;
    }

    @Transactional
    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    public void deleteExpiredOtps() {
        try {
            int deleted = otpRepository.deleteByExpirationTimeBefore(ZonedDateTime.now());
            logger.info("Deleted {} expired OTPs", deleted);
        } catch (Exception e) {
            logger.error("Error deleting expired OTPs: {}", e.getMessage());
        }
    }

    private String generateSecureOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    private void sendOtp(String email, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Your OTP for RailSwad Registration");
            message.setText("Your OTP is: " + otp + ". It is valid for " + OTP_VALIDITY_MINUTES + " minutes. Do not share this code.");
            message.setFrom("your-email@gmail.com"); // Must match spring.mail.username
            javaMailSender.send(message);
            logger.info("OTP email sent to: {}", email);
        } catch (MailException e) {
            logger.error("Error sending OTP email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}