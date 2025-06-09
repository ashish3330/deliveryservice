package com.railswad.deliveryservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class MenuItemDTO {
    private Long itemId;
    private String categoryName; // References MenuCategory.categoryName
    private String itemName;
    private String description;
    private BigDecimal basePrice; // Base price set by platform
    private BigDecimal vendorPrice; // Vendor-specific price (optional)
    private boolean vegetarian;
    private boolean available;
    private Integer preparationTimeMin;
    private String imageUrl;
    private Integer displayOrder;
    private LocalTime availableStartTime;
    private LocalTime availableEndTime;
    private String itemCategory; // Item-specific category (e.g., Gravy, Snack)
}