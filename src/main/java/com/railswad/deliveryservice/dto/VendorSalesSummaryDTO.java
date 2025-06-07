package com.railswad.deliveryservice.dto;

import lombok.Data;

@Data
public class VendorSalesSummaryDTO {
    private Long vendorId;
    private String vendorName;
    private Long totalOrders;
    private Double totalRevenue;
    private Double averageOrderValue;
    private Long deliveredOrders;
    private Long pendingOrders;
    private Long cancelledOrders;

    public VendorSalesSummaryDTO(Long vendorId, String vendorName, Long totalOrders,
                                 Double totalRevenue, Double averageOrderValue,
                                 Long deliveredOrders, Long pendingOrders, Long cancelledOrders) {
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
        this.averageOrderValue = averageOrderValue;
        this.deliveredOrders = deliveredOrders;
        this.pendingOrders = pendingOrders;
        this.cancelledOrders = cancelledOrders;
    }
}