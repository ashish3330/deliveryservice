package com.railswad.deliveryservice.dto;

import lombok.Data;

@Data
public class VendorDTO {
    private Long vendorId; // Output field, set after creation
    private String username; // Input field for User creation
    private String password; // Input field for User creation
    private String businessName;
    private String description;
    private String logoUrl;
    private String fssaiLicense;
    private Long stationId;
    private String address;
    private Integer preparationTimeMin;
    private Double minOrderAmount;
    private boolean verified;
    private Double rating;
    private boolean activeStatus;
}