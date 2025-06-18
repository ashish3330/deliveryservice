package com.railswad.deliveryservice.dto;


import lombok.Data;

@Data
public class CitySalesOverviewDTO {
    private String city;
    private Double totalRevenue;
    private Long orderCount;

    public CitySalesOverviewDTO(String city, Double totalRevenue, Long orderCount) {
        this.city = city;
        this.totalRevenue = totalRevenue;
        this.orderCount = orderCount;
    }
}