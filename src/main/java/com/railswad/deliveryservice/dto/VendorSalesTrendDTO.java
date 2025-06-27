package com.railswad.deliveryservice.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VendorSalesTrendDTO {
    private Long vendorId;
    private String businessName;
    private double totalRevenue;
    // Constructor, getters, setters
}