package com.railswad.deliveryservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.ZonedDateTime;

@Entity
@Table(name = "complaints")
@Data
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complaint_id")
    private Long complaintId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String mobileNumber; // Email or Phone

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status = ComplaintStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    public enum ComplaintStatus {
        OPEN, IN_PROGRESS, RESOLVED
    }
}