package com.example.validation_service.service;

import java.util.List;

public class BusinessValidationException extends Exception {
    private final List<String> validationErrors;

    public BusinessValidationException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public BusinessValidationException(List<String> validationErrors) {
        super("Business validation failed.");
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
