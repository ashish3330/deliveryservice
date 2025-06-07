package com.railswad.deliveryservice.dto;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
public class MenuItemDTO {
    private Long itemId;
    private Long categoryId;
    private String itemName;
    private String description;
    private BigDecimal price;
    private boolean vegetarian;
    private boolean available;
    private Integer preparationTimeMin;
    private String imageUrl;
    private Integer displayOrder;
    private LocalTime availableStartTime;
    private LocalTime availableEndTime;
}