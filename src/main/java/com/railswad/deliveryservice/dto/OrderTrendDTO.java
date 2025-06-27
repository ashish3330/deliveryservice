package com.railswad.deliveryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderTrendDTO {
    private int year;
    private int month;
    private long orderCount;
    private double totalRevenue;
    // Constructor, getters, setters
}