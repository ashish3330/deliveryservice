package com.railswad.deliveryservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuCategoryDTO {
    private Long categoryId;
    private Long vendorId;
    private String categoryName;
    private Integer displayOrder;
}