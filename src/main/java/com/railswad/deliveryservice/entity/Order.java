package com.railswad.deliveryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.*;
import lombok.Data;
import java.time.ZonedDateTime;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @ManyToOne
    @JoinColumn(name = "train_id")
    private Train train;

    @Column(name = "pnr_number")
    private String pnrNumber;

    @Column(name = "coach_number")
    private String coachNumber;

    @Column(name = "seat_number")
    private String seatNumber;

    @ManyToOne
    @JoinColumn(name = "delivery_station_id", nullable = false)
    private Station deliveryStation;

    @Column(name = "delivery_time", nullable = false)
    private ZonedDateTime deliveryTime;

    @Column(name = "order_status", nullable = false)
    private String orderStatus;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "delivery_charges")
    private Double deliveryCharges;

    @Column(name = "tax_amount")
    private Double taxAmount;

    @Column(name = "discount_amount")
    private Double discountAmount;

    @Column(name = "final_amount", nullable = false)
    private Double finalAmount;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Column(name = "delivery_instructions")
    private String deliveryInstructions;
}