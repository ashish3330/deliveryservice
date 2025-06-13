package com.railswad.deliveryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.ZonedDateTime;


@Data
@AllArgsConstructor
public class ComplaintResponseDTO {
    private Long complaintId;
    private Long orderId;
    private Long userId;
    private String name;
    private String email;
    private String mobileNumber;
    private String description;
    private String status;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;


}