package com.railswad.deliveryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CallbackRequestDTO {
    private String name;
    private String email;
    private String mobileNumber;
    private String message;
}