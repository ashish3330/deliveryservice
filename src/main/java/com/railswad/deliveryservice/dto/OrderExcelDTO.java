package com.railswad.deliveryservice.dto;

import lombok.Data;
import java.time.ZonedDateTime;

@Data
public class OrderExcelDTO {
    private Long orderId;
    private String stationName;
    private String vendorName;
    private Integer numberOfItems;
    private Double totalAmount;
    private Double finalAmount;
    private Double taxAmount;
    private String gstNumber;
    private ZonedDateTime orderDate;
    private String paymentMethod;
}