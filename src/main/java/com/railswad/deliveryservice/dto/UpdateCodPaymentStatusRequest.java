package com.railswad.deliveryservice.dto;

import com.railswad.deliveryservice.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class UpdateCodPaymentStatusRequest {
    // Getters and setters
    @NotNull
    private PaymentStatus paymentStatus;
    private String remarks;

}