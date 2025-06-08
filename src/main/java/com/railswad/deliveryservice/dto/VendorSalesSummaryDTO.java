package com.railswad.deliveryservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class VendorSalesSummaryDTO {
    // Getters and setters
    private Long vendorId;
    private String businessName;
    private Long orderCount;
    private Double totalAmount;
    private Double averageOrderValue;

    // Constructor matching the query
    public VendorSalesSummaryDTO(Long vendorId, String businessName, Long orderCount, Double totalAmount, Double averageOrderValue) {
        this.vendorId = vendorId;
        this.businessName = businessName;
        this.orderCount = orderCount;
        this.totalAmount = totalAmount;
        this.averageOrderValue = averageOrderValue;
    }

}