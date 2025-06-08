package com.railswad.deliveryservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.ZonedDateTime;

@Entity
@Table(name = "invoices")
@Data
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Column(name = "invoice_date", nullable = false)
    private ZonedDateTime invoiceDate;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "vendor_name", nullable = false)
    private String vendorName;

    @Column(name = "vendor_gst_number")
    private String vendorGstNumber;

    @Column(name = "subtotal", nullable = false)
    private double subtotal;

    @Column(name = "gst_rate", nullable = false)
    private double gstRate;

    @Column(name = "gst_amount", nullable = false)
    private double gstAmount;

    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus;

    @Column(name = "invoice_data", nullable = false)
    private byte[] invoiceData;
}