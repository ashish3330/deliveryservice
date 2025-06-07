package com.railswad.deliveryservice.dto;


import lombok.Data;

@Data
public class VendorDTO {
    private Long vendorId;
    private String businessName;
    private String description;
    private String logoUrl;
    private String fssaiLicense;
    private Integer stationId;
    private String address;
    private Integer preparationTimeMin;
    private Double minOrderAmount;
    private boolean isVerified;
    private Double rating;
    private boolean activeStatus;
}
