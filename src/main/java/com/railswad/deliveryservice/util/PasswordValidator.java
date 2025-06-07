package com.railswad.deliveryservice.util;

import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

    public static final int MIN_PASSWORD_LENGTH = 8;

    public String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one digit";
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            return "Password must contain at least one special character";
        }
        return null;
    }
}