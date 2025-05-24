package com.example.validation_service.service.rules;

import com.example.validation_service.dto.RawPaymentData;
import com.example.validation_service.service.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

@Component
public class CutoffTimeValidationRule implements ValidationRule {

    private static final Logger logger = LoggerFactory.getLogger(CutoffTimeValidationRule.class);

    private final LocalTime cutoffTime;
    private final ZoneId systemZoneId = ZoneId.systemDefault(); // Assuming server time zone

    public CutoffTimeValidationRule(
            @Value("${validation.rules.cutoff.time:17:00:00}") String cutoffTimeStr) {
        LocalTime tempCutoffTime;
        try {
            tempCutoffTime = LocalTime.parse(cutoffTimeStr);
        } catch (DateTimeParseException e) {
            logger.error("Invalid cutoff time format in properties: '{}'. Defaulting to 17:00:00.", cutoffTimeStr, e);
            tempCutoffTime = LocalTime.of(17, 0, 0);
        }
        this.cutoffTime = tempCutoffTime;
        logger.info("Initialized CutoffTimeValidationRule with cutoff time: {} (Zone: {})", this.cutoffTime, this.systemZoneId);
    }

    @Override
    public void validate(RawPaymentData data, ValidationResult result) {
        // For this rule, we use the current system time when validation is performed.
        // In a real scenario, the message might contain its own creation timestamp,
        // which should ideally be used and converted to the appropriate time zone.
        LocalTime currentTime = LocalTime.now(systemZoneId);

        if (currentTime.isAfter(cutoffTime)) {
            result.addError("Payment processed at " + currentTime + " is after the cutoff time of " + cutoffTime + " in zone " + systemZoneId);
            logger.warn("Cutoff time violation for MsgId {}: Current time {} is after cutoff {}.", data.getMsgId(), currentTime, cutoffTime);
        }
    }
}
