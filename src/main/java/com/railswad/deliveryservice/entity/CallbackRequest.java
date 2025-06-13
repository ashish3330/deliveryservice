package com.railswad.deliveryservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.ZonedDateTime;

@Entity
@Table(name = "callback_requests")
@Data
public class CallbackRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "callback_id")
    private Long callbackId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String mobileNumber;
    // Email or Phone

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallbackStatus status = CallbackStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    public enum CallbackStatus {
        OPEN, IN_PROGRESS, COMPLETED
    }
}