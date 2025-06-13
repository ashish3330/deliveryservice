package com.railswad.deliveryservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@AllArgsConstructor
public class CallbackResponseDTO {
    private Long callbackId;
    private String name;
    private String email;
    private String mobileNumber;
    private String message;
    private String status;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
