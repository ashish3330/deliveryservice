package com.railswad.deliveryservice.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Data
public class BulkOrderDTO {
    private Long bulkOrderId;
    private String name;
    private String email;
    private String phone;
    private String deliveryStation;
    private String orderDetails;
    private Integer quantity;
    private LocalDateTime createdAt;
}