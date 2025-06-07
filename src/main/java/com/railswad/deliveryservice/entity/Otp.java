package com.railswad.deliveryservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.ZonedDateTime;

@Entity
@Table(name = "otps")
@Data
public class Otp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otp_id")
    private Long otpId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "otp_code", nullable = false)
    private String otpCode;

    @Column(name = "otp_type", nullable = false)
    private String otpType;

    @Column(name = "is_used")
    private boolean isUsed;

    @Column(name = "expiration_time", nullable = false)
    private ZonedDateTime expirationTime;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "attempt_count")
    private int attemptCount;
}