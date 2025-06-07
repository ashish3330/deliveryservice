package com.railswad.deliveryservice.dto;

import lombok.Data;

@Data
public class OtpRequestDTO {
    private String email;
    private String otpCode;
}
