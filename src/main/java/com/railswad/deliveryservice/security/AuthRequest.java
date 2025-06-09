package com.railswad.deliveryservice.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.ScriptAssert;

@ScriptAssert(lang = "javascript", script = "_this.identifier != null", message = "Either email or phone number is required")
@Data
public class AuthRequest {
    @NotBlank(message = "Identifier is required")
    private String identifier; // Can be either email or phoneNumber

    @NotBlank(message = "Password is required")
    private String password;
}