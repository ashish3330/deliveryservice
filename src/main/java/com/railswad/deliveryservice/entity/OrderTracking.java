package com.railswad.deliveryservice.entity;


import jakarta.persistence.*;
import lombok.Data;
import java.time.ZonedDateTime;

@Entity
@Table(name = "order_tracking")
@Data
public class OrderTracking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long trackingId;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private String status;

    private String remarks;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "updated_by")
    private User updatedBy;
}
