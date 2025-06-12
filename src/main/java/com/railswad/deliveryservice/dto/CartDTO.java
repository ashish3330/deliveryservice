package com.railswad.deliveryservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class CartDTO {
    private String cartId;
    private Long customerId;
    private Long vendorId;
    private Long trainId;
    private String pnrNumber;
    private String coachNumber;
    private String seatNumber;
    private Long deliveryStationId;
    private List<CartItemDTO> items;
    private String deliveryInstructions;
}