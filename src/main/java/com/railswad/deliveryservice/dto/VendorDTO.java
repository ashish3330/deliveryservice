package com.railswad.deliveryservice.dto;

import lombok.Data;

@Data
public class VendorDTO {
    private Long vendorId; // Output field, set after creation
    private String username; // Input field for User creation
    private String email; // Input field for User creation
    private String businessName;
    private String description;
    private String logoUrl;
    private String fssaiLicense;
    private String gstNumber;
    private Long stationId;
    private String address;
    private Integer preparationTimeMin;
    private Double minOrderAmount;
    private boolean verified;
    private boolean isVeg;
    private Double rating;
    private boolean activeStatus;
}