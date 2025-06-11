package com.railswad.deliveryservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railswad.deliveryservice.dto.UserDTO;
import com.railswad.deliveryservice.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    private static final int OTP_VALIDITY_SECONDS = 300; // 5 minutes
    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final int OTP_LENGTH = 6;
    private static final int RATE_LIMIT_SECONDS = 60; // 1 minute
    private static final String OTP_TYPE = "REGISTRATION";
    private static final String REDIS_OTP_PREFIX = "temp:otp:";
    private static final String REDIS_RATE_LIMIT_PREFIX = "temp:rate:otp:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${otp.sender.email:no-reply@railswad.com}")
    private String senderEmail;

    // Placeholder for SMS provider configuration
    @Value("${otp.sender.sms.api-key:}")
    private String smsApiKey;

    public void generateAndSendOtp(String tempUserId, String ipAddress, String deviceInfo) {
        logger.info("Generating OTP for tempUserId: {}", tempUserId);

        // Retrieve UserDTO from Redis
        String redisUserKey = "temp:user:" + tempUserId;
        String userJson = redisTemplate.opsForValue().get(redisUserKey);
        if (userJson == null) {
            logger.warn("No user data found in Redis for tempUserId: {}", tempUserId);
            throw new ServiceException("USER_DATA_NOT_FOUND", "User data not found");
        }

        UserDTO userDTO;
        try {
            userDTO = objectMapper.readValue(userJson, UserDTO.class);
        } catch (Exception e) {
            logger.error("Failed to deserialize UserDTO for tempUserId: {}: {}", tempUserId, e.getMessage(), e);
            throw new ServiceException("REDIS_DESERIALIZATION_FAILED", "Failed to retrieve user data");
        }

        String identifier = userDTO.getEmail() != null && !userDTO.getEmail().isEmpty() ?
                userDTO.getEmail() : userDTO.getPhoneNumber();
        if (identifier == null) {
            logger.warn("No email or phone number found for tempUserId: {}", tempUserId);
            throw new ServiceException("INVALID_USER_DATA", "Email or phone number required");
        }

        // Check rate limiting
        String rateLimitKey = REDIS_RATE_LIMIT_PREFIX + identifier;
        String rateLimit = redisTemplate.opsForValue().get(rateLimitKey);
        if (rateLimit != null) {
            logger.warn("Rate limit exceeded for identifier: {}", identifier);
            throw new ServiceException("RATE_LIMIT_EXCEEDED", "Please wait before requesting a new OTP");
        }

        try {
            String otp = generateSecureOtp();
            Map<String, String> otpData = new HashMap<>();
            otpData.put("otpCode", otp);
            otpData.put("otpType", OTP_TYPE);
            otpData.put("createdAt", ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
            otpData.put("used", "false");
            otpData.put("attemptCount", "0");
            otpData.put("ipAddress", ipAddress);
            otpData.put("deviceInfo", deviceInfo);

            // Store OTP in Redis
            String redisOtpKey = REDIS_OTP_PREFIX + tempUserId;
            redisTemplate.opsForHash().putAll(redisOtpKey, otpData);
            redisTemplate.expire(redisOtpKey, OTP_VALIDITY_SECONDS, TimeUnit.SECONDS);

            // Set rate limit
            redisTemplate.opsForValue().set(rateLimitKey, "1", RATE_LIMIT_SECONDS, TimeUnit.SECONDS);

            // Send OTP
            if (userDTO.getEmail() != null && !userDTO.getEmail().isEmpty()) {
                sendOtpEmail(userDTO.getEmail(), otp);
            } else {
                sendOtpSms(userDTO.getPhoneNumber(), otp);
            }
            logger.info("OTP {} generated and sent for tempUserId: {}", otp, tempUserId);
        } catch (Exception e) {
            logger.error("Failed to generate OTP for tempUserId: {}: {}", tempUserId, e.getMessage(), e);
            // Clean up Redis rate limit on failure
            redisTemplate.delete(rateLimitKey);
            throw new ServiceException("OTP_GENERATION_FAILED", "Failed to generate OTP");
        }
    }

    public boolean verifyOtp(String tempUserId, String otpCode, String ipAddress) {
        logger.info("Verifying OTP for tempUserId: {}", tempUserId);

        String redisOtpKey = REDIS_OTP_PREFIX + tempUserId;
        Map<Object, Object> otpData = redisTemplate.opsForHash().entries(redisOtpKey);
        if (otpData == null || otpData.isEmpty()) {
            logger.warn("No OTP found for tempUserId: {}", tempUserId);
            return false;
        }

        String storedOtp = (String) otpData.get("otpCode");
        String otpType = (String) otpData.get("otpType");
        String usedStr = (String) otpData.get("used");
        String attemptCountStr = (String) otpData.get("attemptCount");
        String createdAtStr = (String) otpData.get("createdAt");

        if (!OTP_TYPE.equals(otpType)) {
            logger.warn("Invalid OTP type for tempUserId: {}", tempUserId);
            return false;
        }

        boolean used = Boolean.parseBoolean(usedStr);
        if (used) {
            logger.warn("OTP already used for tempUserId: {}", tempUserId);
            return false;
        }

        ZonedDateTime createdAt = ZonedDateTime.parse(createdAtStr, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        if (ZonedDateTime.now().isAfter(createdAt.plusSeconds(OTP_VALIDITY_SECONDS))) {
            logger.warn("Expired OTP for tempUserId: {}", tempUserId);
            redisTemplate.delete(redisOtpKey);
            return false;
        }

        int attemptCount = Integer.parseInt(attemptCountStr) + 1;
        redisTemplate.opsForHash().put(redisOtpKey, "attemptCount", String.valueOf(attemptCount));
        redisTemplate.opsForHash().put(redisOtpKey, "ipAddress", ipAddress);

        if (attemptCount >= MAX_OTP_ATTEMPTS) {
            logger.warn("Max OTP attempts exceeded for tempUserId: {}", tempUserId);
            redisTemplate.delete(redisOtpKey);
            return false;
        }

        if (!otpCode.equals(storedOtp)) {
            logger.warn("Incorrect OTP for tempUserId: {}", tempUserId);
            return false;
        }

        // Mark OTP as used
        redisTemplate.opsForHash().put(redisOtpKey, "used", "true");
        logger.info("OTP verified successfully for tempUserId: {}", tempUserId);
        return true;
    }

    private String generateSecureOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    private void sendOtpEmail(String email, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Your OTP for RailSwad Registration");
            message.setText("Your OTP is: " + otp + ". It is valid for " + (OTP_VALIDITY_SECONDS / 60) + " minutes. Do not share this code.");
            message.setFrom(senderEmail);
            javaMailSender.send(message);
            logger.info("Sent OTP email to: {}", email);
        } catch (MailException e) {
            logger.error("Failed to send OTP email to {}: {}", email, e.getMessage(), e);
            throw new ServiceException("EMAIL_FAILED", "Failed to send OTP email");
        }
    }

    private void sendOtpSms(String phoneNumber, String otp) {
        // Placeholder for SMS provider (e.g., Twilio, AWS SNS)
        if (smsApiKey.isEmpty()) {
            logger.warn("SMS provider not configured for phone: {}. OTP: {}", phoneNumber, otp);
            throw new ServiceException("SMS_NOT_CONFIGURED", "SMS provider not configured");
        }
        // Example: Twilio implementation
        /*
        try {
            Twilio.init(smsAccountSid, smsAuthToken);
            Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber("YOUR_TWILIO_NUMBER"),
                    "Your OTP is: " + otp + ". It is valid for " + (OTP_TTL_SECONDS / 60) + " minutes."
            ).create();
            logger.info("Sent OTP SMS to: {}", phoneNumber);
        } catch (Exception e) {
            logger.error("Failed to send OTP SMS to {}: {}", phoneNumber, e.getMessage());
            throw new ServiceException("SMS_FAILED", "Failed to send OTP SMS");
        }
        */
        logger.warn("SMS provider integration required for phone: {}. OTP: {}", phoneNumber, otp);
        throw new ServiceException("SMS_NOT_IMPLEMENTED", "SMS provider integration required");
    }
}