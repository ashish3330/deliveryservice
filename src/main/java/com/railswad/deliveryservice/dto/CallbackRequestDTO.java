package com.railswad.deliveryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallbackRequestDTO {
    private String name;
    private String email;
    private String mobileNumber;
    private String message;
}