package com.railswad.deliveryservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Entity
@Getter
@Table(name = "vendors")
public class Vendor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vendorId;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    @Column(name = "business_name")
    private String businessName;
    private String description;
    @Column(name = "logo_url")
    private String logoUrl;
    @Column(name = "fssai_license")
    private String fssaiLicense;

    @Column(name = "gst_number")
    private String gstNumber;
    @ManyToOne
    @JoinColumn(name = "station_id")
    private Station station;
    private String address;
    @Column(name = "preparation_time_min")
    private Integer preparationTimeMin;
    @Column(name = "min_order_amount")
    private Double minOrderAmount;
    private Boolean verified;
    private Boolean isVeg;
    private Double rating;
    @Column(name = "active_status")
    private Boolean activeStatus;
}