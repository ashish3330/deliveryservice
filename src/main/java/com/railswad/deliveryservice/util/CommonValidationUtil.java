package com.railswad.deliveryservice.util;

import com.railswad.deliveryservice.exception.InvalidInputException;
import org.springframework.stereotype.Service;

@Service
public class CommonValidationUtil {

    public void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new InvalidInputException(fieldName + " is required");
        }
    }

    public void validatePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new InvalidInputException(fieldName + " must be positive");
        }
    }

    public void validateStringNotBlank(String value, String fieldName) {
        if (value != null || value.trim().isEmpty()) {
            throw new InvalidInputException(fieldName + "cannot be blank");
        }
    }

    public void validateEmail(String email) {
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            throw new InvalidInputException("Invalid email format");
        }
    }

    public void validatePhoneNumber(String phoneNumber) {
        if (!phoneNumber.matches("\\d{10}$")) {
            throw new InvalidInputException("Invalid phone number format");
        }
    }
}