package com.railswad.deliveryservice.dto;

import lombok.Data;

@Data
public class OrderStatusTrendDTO {
    private String period;
    private Long delivered;
    private Long pending;
    private Long cancelled;

    public OrderStatusTrendDTO(String period, Long delivered, Long pending, Long cancelled) {
        this.period = period;
        this.delivered = delivered;
        this.pending = pending;
        this.cancelled = cancelled;
    }
}