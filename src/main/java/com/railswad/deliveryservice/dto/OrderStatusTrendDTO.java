package com.railswad.deliveryservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.text.SimpleDateFormat;
import java.util.Date;

@Setter
@Getter
public class OrderStatusTrendDTO {
    // Getters and setters
    private String date; // "YYYY-MM" or "YYYY-MM-DD" depending on groupBy
    private Long deliveredCount;
    private Long pendingCount;
    private Long cancelledCount;

    // Constructor to handle both Date and String inputs
    public OrderStatusTrendDTO(Object date, Long deliveredCount, Long pendingCount, Long cancelledCount) {
        if (date instanceof Date) {
            this.date = new SimpleDateFormat("yyyy-MM-dd").format((Date) date);
        } else {
            this.date = date.toString(); // Handles String like "2025-06"
        }
        this.deliveredCount = deliveredCount;
        this.pendingCount = pendingCount;
        this.cancelledCount = cancelledCount;
    }

}