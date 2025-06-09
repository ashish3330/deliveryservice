package com.railswad.deliveryservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorCreationDTO {
    // User-related fields
    private String email;
    private String username;
    private String phone;
    private String password;

    // Vendor-related fields
    private String businessName;
    private String description;
    private String logoUrl;
    private String fssaiLicense;
    private Long stationId;
    private String address;
    private Integer preparationTimeMin;
    private Double minOrderAmount;
    private Boolean verified;
    private Double rating;
    private Boolean activeStatus;
}