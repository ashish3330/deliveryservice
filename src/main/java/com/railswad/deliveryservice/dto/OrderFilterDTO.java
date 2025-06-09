package com.railswad.deliveryservice.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrderFilterDTO {
    private Long stationId;
    private Boolean isVegetarianMenuItem;
    private Boolean isVegetarianVendor;

}