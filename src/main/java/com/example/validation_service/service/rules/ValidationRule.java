package com.example.validation_service.service.rules;

import com.example.validation_service.dto.RawPaymentData;
import com.example.validation_service.service.ValidationResult;

public interface ValidationRule {
    void validate(RawPaymentData data, ValidationResult result);
}
