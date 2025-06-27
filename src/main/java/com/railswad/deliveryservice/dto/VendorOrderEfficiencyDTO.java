package com.railswad.deliveryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VendorOrderEfficiencyDTO {
    private Long vendorId;
    private String businessName;
    private double averageDeliveryTimeHours;
    // Constructor, getters, setters
}