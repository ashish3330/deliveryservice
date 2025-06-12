package com.railswad.deliveryservice.dto;

import lombok.Data;

@Data
public class AddItemRequest {
    private Long vendorId;
    private Long itemId;
    private Integer quantity;
    private String specialInstructions;
    private Long trainId;
    private String pnrNumber;
    private String coachNumber;
    private String seatNumber;
    private Long deliveryStationId;
    private String deliveryInstructions;
}