package com.example.validation_service.service;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private final List<String> errors = new ArrayList<>();

    public void addError(String error) {
        this.errors.add(error);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors); // Return a copy
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
               "errors=" + errors +
               '}';
    }
}
