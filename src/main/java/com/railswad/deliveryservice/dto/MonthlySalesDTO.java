package com.railswad.deliveryservice.dto;

import lombok.Data;

@Data
public class MonthlySalesDTO {
    private String month;
    private Long totalOrders;
    private Double totalRevenue;

    public MonthlySalesDTO(String month, Long totalOrders, Double totalRevenue) {
        this.month = month;
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
    }
}