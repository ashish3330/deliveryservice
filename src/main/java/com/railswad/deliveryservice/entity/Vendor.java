package com.railswad.deliveryservice.entity;

import com.railswad.deliveryservice.entity.MenuCategory;
import com.railswad.deliveryservice.entity.Station;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "vendors")
@Data
public class Vendor {
    @Id
    private Long vendorId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "vendor_id")
    private User user;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "fssai_license", nullable = false)
    private String fssaiLicense;

    @ManyToOne
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(nullable = false)
    private String address;

    @Column(name = "preparation_time_min")
    private Integer preparationTimeMin;

    @Column(name = "min_order_amount")
    private Double minOrderAmount;

    @Column(name = "is_verified")
    private boolean isVerified;

    private Double rating;

    @Column(name = "active_status")
    private boolean activeStatus;

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MenuCategory> menuCategories;
}
