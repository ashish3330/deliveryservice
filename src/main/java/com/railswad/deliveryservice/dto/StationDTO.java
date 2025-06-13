package com.railswad.deliveryservice.dto;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
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