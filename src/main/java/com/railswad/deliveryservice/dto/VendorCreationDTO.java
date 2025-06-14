package com.railswad.deliveryservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private String gstNumber;
    private Long stationId;
    private String address;
    private Integer preparationTimeMin;
    private Double minOrderAmount;
    private Boolean verified;
    private Boolean isVeg;
    private Double rating;
    private Boolean activeStatus;
}