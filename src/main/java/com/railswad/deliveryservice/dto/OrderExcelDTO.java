package com.railswad.deliveryservice.dto;

import lombok.Data;

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
}