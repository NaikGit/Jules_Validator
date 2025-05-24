package com.example.validation_service.service.rules;

import com.example.validation_service.dto.RawPaymentData;
import com.example.validation_service.service.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CurrencyValidationRule implements ValidationRule {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyValidationRule.class);

    private final Set<String> allowedCurrencies;

    public CurrencyValidationRule(
            @Value("${validation.rules.currency.allowed:USD,EUR,GBP}") String allowedCurrenciesCsv) {
        if (allowedCurrenciesCsv != null && !allowedCurrenciesCsv.isEmpty()) {
            this.allowedCurrencies = Arrays.stream(allowedCurrenciesCsv.split(","))
                                           .map(String::trim)
                                           .filter(s -> !s.isEmpty())
                                           .collect(Collectors.toSet());
        } else {
            this.allowedCurrencies = new HashSet<>(Arrays.asList("USD", "EUR", "GBP")); // Default if property is empty
        }
        logger.info("Initialized CurrencyValidationRule with allowed currencies: {}", this.allowedCurrencies);
    }

    @Override
    public void validate(RawPaymentData data, ValidationResult result) {
        if (data.getCurrency() == null || data.getCurrency().trim().isEmpty()) {
            result.addError("Currency is missing.");
            return;
        }

        String currency = data.getCurrency().trim().toUpperCase();
        if (!allowedCurrencies.contains(currency)) {
            result.addError("Currency " + currency + " is not allowed. Allowed currencies are: " + allowedCurrencies);
        }
    }
}
