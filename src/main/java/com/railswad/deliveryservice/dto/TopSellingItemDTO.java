package com.railswad.deliveryservice.dto;

import lombok.Data;

@Data
public class TopSellingItemDTO {
    private Long itemId;
    private String itemName;
    private String category;
    private Long quantitySold;
    private Double totalRevenue;

    public TopSellingItemDTO(Long itemId, String itemName, String category,
                             Long quantitySold, Double totalRevenue) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.category = category;
        this.quantitySold = quantitySold;
        this.totalRevenue = totalRevenue;
    }
}