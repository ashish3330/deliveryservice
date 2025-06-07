package com.railswad.deliveryservice.dto;

import lombok.Data;

@Data
public class StationSalesOverviewDTO {
    private Long stationId;
    private String stationName;
    private Long totalOrders;
    private Double totalRevenue;

    public StationSalesOverviewDTO(Long stationId, String stationName, Long totalOrders, Double totalRevenue) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
    }
}