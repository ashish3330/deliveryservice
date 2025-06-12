package com.railswad.deliveryservice.dto;

import com.railswad.deliveryservice.entity.OrderStatus;
import com.railswad.deliveryservice.entity.PaymentStatus;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class OrderDTO {
    private Long orderId;
    private Long customerId;
    private Long vendorId;
    private Long trainId;
    private String pnrNumber;
    private String coachNumber;
    private String seatNumber;
    private Long deliveryStationId;
    private ZonedDateTime deliveryTime;
    private OrderStatus orderStatus;
    private Double totalAmount;
    private Double deliveryCharges;
    private Double taxAmount;
    private Double discountAmount;
    private Double finalAmount;
    private PaymentStatus paymentStatus;
    private String paymentMethod;
    private String razorpayOrderID;
    private String deliveryInstructions;
    private List<OrderItemDTO> items;
}