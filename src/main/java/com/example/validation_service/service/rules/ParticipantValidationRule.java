package com.example.validation_service.service.rules;

import com.example.validation_service.dto.RawPaymentData;
import com.example.validation_service.service.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ParticipantValidationRule implements ValidationRule {

    private static final Logger logger = LoggerFactory.getLogger(ParticipantValidationRule.class);

    private final Set<String> supportedPrefixes;

    public ParticipantValidationRule(
            @Value("${validation.rules.participant.supported-prefixes:BANK,CUST}") String prefixesCsv) {
        if (prefixesCsv != null && !prefixesCsv.isEmpty()) {
            this.supportedPrefixes = Arrays.stream(prefixesCsv.split(","))
                                           .map(String::trim)
                                           .filter(s -> !s.isEmpty())
                                           .collect(Collectors.toSet());
        } else {
            this.supportedPrefixes = new HashSet<>(Arrays.asList("BANK", "CUST")); // Default if property is empty
        }
        logger.info("Initialized ParticipantValidationRule with supported prefixes: {}", this.supportedPrefixes);
    }

    @Override
    public void validate(RawPaymentData data, ValidationResult result) {
        validateParticipantId(data.getDebtorId(), "Debtor ID", result);
        validateParticipantId(data.getCreditorId(), "Creditor ID", result);
    }

    private void validateParticipantId(String participantId, String fieldName, ValidationResult result) {
        if (!StringUtils.hasText(participantId)) {
            // Allowing empty participant IDs as they are not strictly mandatory in all pain.001 variants
            // and schema validation already checks for presence of names.
            // If specific prefixes are expected IF an ID is present, this logic can be adjusted.
            // result.addError(fieldName + " is missing or empty.");
            return;
        }

        boolean prefixMatches = supportedPrefixes.stream().anyMatch(prefix -> participantId.startsWith(prefix));
        if (!prefixMatches) {
            result.addError(fieldName + " '" + participantId + "' does not start with a supported prefix. Supported prefixes: " + supportedPrefixes);
        }
    }
}
