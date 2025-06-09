package com.railswad.deliveryservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private MenuCategory category; // Reference to MenuCategory

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "description")
    private String description;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice; // Base price set by platform

    @Column(name = "vendor_price")
    private BigDecimal vendorPrice; // Vendor-specific price (optional)

    @Column(name = "is_vegetarian", nullable = false)
    private boolean vegetarian;

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    @Column(name = "preparation_time_min")
    private Integer preparationTimeMin;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "available_start_time")
    private LocalTime availableStartTime;

    @Column(name = "available_end_time")
    private LocalTime availableEndTime;

    @Column(name = "item_category", length = 50)
    private String itemCategory; // Item-specific category (e.g., Gravy, Snack)

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;

    @PrePersist
    @PreUpdate
    private void validateTimesAndPrices() {
        if (availableStartTime != null && availableEndTime != null) {
            if (availableEndTime.isBefore(availableStartTime)) {
                throw new IllegalArgumentException("End time must be after start time");
            }
        }
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base price must be non-negative");
        }
        if (vendorPrice != null) {
            if (vendorPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Vendor price cannot be negative");
            }
            if (basePrice.compareTo(vendorPrice) <= 0) {
                throw new IllegalArgumentException("Base price must be greater than vendor price");
            }
        }
    }
}