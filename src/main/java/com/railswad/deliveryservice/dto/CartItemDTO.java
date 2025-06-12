package com.railswad.deliveryservice.dto;

import lombok.Data;

@Data
public class CartItemDTO {
    private Long itemId;
    private Integer quantity;
    private Double unitPrice;
    private String specialInstructions;
}