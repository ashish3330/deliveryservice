package com.railswad.deliveryservice.exception;


import lombok.Getter;
import lombok.Setter;

@Getter
public class ServiceException extends RuntimeException {
    private final String errorCode;
    private final String message;

    public ServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
    }
}