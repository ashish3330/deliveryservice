package com.railswad.deliveryservice.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StationSalesSummaryDTO {
    // Getters and setters
    private Integer stationId;
    private String stationName;
    private Long orderCount;
    private Double totalAmount;
    private Double averageOrderValue;

    // Constructor matching the query
    public StationSalesSummaryDTO(Integer stationId, String stationName, Long orderCount, Double totalAmount, Double averageOrderValue) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.orderCount = orderCount;
        this.totalAmount = totalAmount;
        this.averageOrderValue = averageOrderValue;
    }

}