package com.railswad.deliveryservice.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StationSalesOverviewDTO {
    // Getters and setters
    private Integer stationId;
    private String stationName;
    private Long orderCount;
    private Double totalAmount;

    // Constructor matching the query
    public StationSalesOverviewDTO(Integer stationId, String stationName, Long orderCount, Double totalAmount) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.orderCount = orderCount;
        this.totalAmount = totalAmount;
    }

}