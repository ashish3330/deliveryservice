package com.railswad.deliveryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.ScriptAssert;
import lombok.Data;

@ScriptAssert(lang = "javascript", script = "_this.email != null || _this.phoneNumber != null", message = "Either email or phoneNumber is required")
@Data
public class OtpRequestDTO {
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "OTP is required")
    private String otp;
}