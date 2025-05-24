package com.example.validation_service.service;

import com.example.validation_service.dto.RawPaymentData;
import com.example.validation_service.service.rules.ValidationRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusinessRuleValidatorService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessRuleValidatorService.class);

    private final List<ValidationRule> validationRules;

    @Autowired
    public BusinessRuleValidatorService(List<ValidationRule> validationRules) {
        this.validationRules = validationRules;
        logger.info("BusinessRuleValidatorService initialized with {} rules.", validationRules.size());
        validationRules.forEach(rule -> logger.info("Registered rule: {}", rule.getClass().getSimpleName()));
    }

    public void validate(RawPaymentData data) throws BusinessValidationException {
        ValidationResult validationResult = new ValidationResult();

        for (ValidationRule rule : validationRules) {
            try {
                logger.debug("Applying rule: {}", rule.getClass().getSimpleName());
                rule.validate(data, validationResult);
            } catch (Exception e) {
                // Catching unexpected exceptions during a single rule execution
                logger.error("Error executing validation rule: {}", rule.getClass().getSimpleName(), e);
                validationResult.addError("System error during validation rule " + rule.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        if (validationResult.hasErrors()) {
            logger.warn("Business validation failed for MsgId {}: {}", data.getMsgId(), validationResult.getErrors());
            throw new BusinessValidationException(validationResult.getErrors());
        }

        logger.info("Business validation successful for MsgId {}", data.getMsgId());
    }
}
