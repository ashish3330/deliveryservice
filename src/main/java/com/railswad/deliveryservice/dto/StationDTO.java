package com.railswad.deliveryservice.dto;


import lombok.Data;

@Data
public class StationDTO {
    private Integer stationId;
    private String stationCode;
    private String stationName;
    private String city;
    private String state;
    private String pincode;
    private Double latitude;
    private Double longitude;
}