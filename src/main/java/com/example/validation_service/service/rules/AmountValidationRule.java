package com.example.validation_service.service.rules;

import com.example.validation_service.dto.RawPaymentData;
import com.example.validation_service.service.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AmountValidationRule implements ValidationRule {

    private static final Logger logger = LoggerFactory.getLogger(AmountValidationRule.class);

    private final BigDecimal minAmount;
    private final BigDecimal maxAmount;

    public AmountValidationRule(
            @Value("${validation.rules.amount.min:0.01}") String minAmountStr,
            @Value("${validation.rules.amount.max:1000000.00}") String maxAmountStr) {
        this.minAmount = new BigDecimal(minAmountStr);
        this.maxAmount = new BigDecimal(maxAmountStr);
        logger.info("Initialized AmountValidationRule with min: {}, max: {}", this.minAmount, this.maxAmount);
    }

    @Override
    public void validate(RawPaymentData data, ValidationResult result) {
        if (data.getAmount() == null || data.getAmount().trim().isEmpty()) {
            result.addError("Amount is missing.");
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(data.getAmount());
            if (amount.compareTo(minAmount) < 0) {
                result.addError("Amount " + amount + " is less than minimum allowed " + minAmount);
            }
            if (amount.compareTo(maxAmount) > 0) {
                result.addError("Amount " + amount + " is greater than maximum allowed " + maxAmount);
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid amount format for MsgId {}: {}", data.getMsgId(), data.getAmount(), e);
            result.addError("Invalid amount format: " + data.getAmount());
        }
    }
}
