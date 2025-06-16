package com.railswad.deliveryservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class CartSummaryDTO {
    private String cartId;
    private Long customerId;
    private Long vendorId;
    private Long trainId;
    private String pnrNumber;
    private String coachNumber;
    private String seatNumber;
    private Double subtotal;
    private Double taxAmount;
    private Double deliveryCharges;
    private Double finalAmount;
    private List<CartItemDTO> items;
}
