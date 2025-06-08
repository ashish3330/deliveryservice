package com.railswad.deliveryservice.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class VendorSalesOverviewDTO {
    // Getters and setters
    private Long vendorId;
    private String businessName;
    private Long orderCount;
    private Double totalAmount;

    // Constructor matching the query
    public VendorSalesOverviewDTO(Long vendorId, String businessName, Long orderCount, Double totalAmount) {
        this.vendorId = vendorId;
        this.businessName = businessName;
        this.orderCount = orderCount;
        this.totalAmount = totalAmount;
    }

}