package com.railswad.deliveryservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemTrendDTO {
    private String itemName;
    private int year;
    private int month;
    private long quantity;
    // Constructor, getters, setters
}
