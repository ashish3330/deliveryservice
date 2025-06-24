package com.railswad.deliveryservice.dto;


import com.railswad.deliveryservice.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateOrderStatusRequest {
    // Getters and setters
    @NotNull
    private OrderStatus status;
    private String remarks;

}