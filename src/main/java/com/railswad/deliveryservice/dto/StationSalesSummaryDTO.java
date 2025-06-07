package com.railswad.deliveryservice.dto;


import lombok.Data;

@Data
public class StationSalesSummaryDTO {
    private Long stationId;
    private String stationName;
    private Long totalOrders;
    private Double totalRevenue;
    private Double averageOrderValue;

    public StationSalesSummaryDTO(Long stationId, String stationName, Long totalOrders,
                                  Double totalRevenue, Double averageOrderValue) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
        this.averageOrderValue = averageOrderValue;
    }
}
