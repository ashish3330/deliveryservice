package com.railswad.deliveryservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StationOrderDensityDTO {
    private Integer stationId;
    private String stationName;
    private long orderCount;
    // Constructor, getters, setters
}
