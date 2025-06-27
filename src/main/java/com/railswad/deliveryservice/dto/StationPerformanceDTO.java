package com.railswad.deliveryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StationPerformanceDTO {
    private Integer stationId;
    private String stationName;
    private long orderCount;
    private double totalRevenue;
    // Constructor, getters, setters
}