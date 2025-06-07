package com.railswad.deliveryservice.entity;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "stations")
@Data
public class Station {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer stationId;

    @Column(name = "station_code", unique = true, nullable = false)
    private String stationCode;

    @Column(name = "station_name", nullable = false)
    private String stationName;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    private String pincode;

    private Double latitude;

    private Double longitude;
}
