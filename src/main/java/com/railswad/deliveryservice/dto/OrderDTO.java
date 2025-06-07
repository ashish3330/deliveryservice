package com.railswad.deliveryservice.dto;


import lombok.Data;
import java.time.ZonedDateTime;
import java.util.List;

@Data
public class OrderDTO {
    private Long orderId;
    private Long customerId;
    private Long vendorId;
    private Integer trainId;
    private String pnrNumber;
    private String coachNumber;
    private String seatNumber;
    private Integer deliveryStationId;
    private ZonedDateTime deliveryTime;
    private String orderStatus;
    private Double totalAmount;
    private Double deliveryCharges;
    private Double taxAmount;
    private Double discountAmount;
    private Double finalAmount;
    private String paymentStatus;
    private String paymentMethod;
    private String deliveryInstructions;
    private List<OrderItemDTO> items;
}