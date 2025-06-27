package com.railswad.deliveryservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class VendorRatingDTO {
    private Long vendorId;
    private String businessName;
    private Double averageRating;
    // Constructor, getters, setters
}