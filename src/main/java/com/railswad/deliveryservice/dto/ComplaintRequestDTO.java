package com.railswad.deliveryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.checkerframework.checker.units.qual.A;

@Data
@AllArgsConstructor
public class ComplaintRequestDTO {
    private Long orderId;
    private String name;
    private String email;
    private String mobileNumber;
    private String description;
}