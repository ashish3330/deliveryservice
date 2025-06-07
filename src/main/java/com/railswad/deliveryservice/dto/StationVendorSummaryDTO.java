package com.railswad.deliveryservice.dto;

import lombok.Data;

@Data
public class StationVendorSummaryDTO {
    private Long stationId;
    private String stationName;
    private Long vendorCount;
    private VendorSalesOverviewDTO topVendor;

    public StationVendorSummaryDTO(Long stationId, String stationName, Long vendorCount) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.vendorCount = vendorCount;
    }
}