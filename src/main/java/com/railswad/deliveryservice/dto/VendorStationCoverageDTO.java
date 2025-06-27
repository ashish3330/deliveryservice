package com.railswad.deliveryservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VendorStationCoverageDTO {
    private Long vendorId;
    private String businessName;
    private long stationCount;
    // Constructor, getters, setters
}