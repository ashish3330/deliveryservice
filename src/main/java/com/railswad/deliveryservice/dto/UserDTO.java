package com.railswad.deliveryservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.ScriptAssert;
import lombok.Data;

@ScriptAssert(lang = "javascript", script = "_this.email != null || _this.phoneNumber != null", message = "Either email or phoneNumber is required")
@Data
public class UserDTO {
    @Email(message = "Invalid email format", regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    private String password;

    private String username;

    private String role; // Role to be assigned during creation
}