package com.railswad.deliveryservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemCategoryPerformanceDTO {
    private String categoryName;
    private long totalQuantity;
    private double totalRevenue;
    // Constructor, getters, setters
}

