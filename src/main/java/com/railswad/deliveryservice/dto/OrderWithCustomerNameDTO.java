package com.railswad.deliveryservice.dto;


import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderWithCustomerNameDTO extends OrderDTO {
    private String customerName;
}