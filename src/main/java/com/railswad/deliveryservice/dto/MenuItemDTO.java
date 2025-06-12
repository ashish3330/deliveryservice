package com.railswad.deliveryservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
public class MenuItemDTO {
    private Long itemId;
    private Long categoryId;
    private String categoryName;
    private String itemName;
    private String description;
    private BigDecimal basePrice;
    private BigDecimal vendorPrice;
    private boolean vegetarian;
    private boolean available = true;
    private Integer preparationTimeMin;
    private String imageUrl;
    private Integer displayOrder;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime availableStartTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime availableEndTime;
    private String itemCategory;
}