package com.railswad.deliveryservice.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {
    @NotBlank(message = "Identifier is required")
    private String identifier; // Can be either email or phoneNumber

    @NotBlank(message = "Password is required")
    private String password;
}