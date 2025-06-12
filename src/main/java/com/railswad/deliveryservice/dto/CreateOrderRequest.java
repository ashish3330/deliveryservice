package com.railswad.deliveryservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Setter
@Getter
public  class CreateOrderRequest {
    private String cartId;
    private Long vendorId;
    private Long trainId;
    private String pnrNumber;
    private String coachNumber;
    private String seatNumber;
    private Long deliveryStationId;
    private String deliveryInstructions;
    private String paymentMethod;
    private ZonedDateTime deliveryTime;
}
