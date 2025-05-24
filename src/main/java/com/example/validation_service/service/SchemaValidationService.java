package com.example.validation_service.service;

import com.example.validation_service.dto.RawPaymentData;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils; // Using Spring's StringUtils for checking text

import java.util.ArrayList;
import java.util.List;

@Service
public class SchemaValidationService {

    public void validate(RawPaymentData rawData) throws SchemaValidationException {
        if (rawData == null) {
            throw new SchemaValidationException("RawPaymentData object cannot be null.");
        }

        List<String> validationErrors = new ArrayList<>();

        // Check for presence of mandatory fields
        if (!StringUtils.hasText(rawData.getMsgId())) {
            validationErrors.add("MsgId is missing or empty.");
        }
        if (!StringUtils.hasText(rawData.getAmount())) {
            validationErrors.add("Amount (Amt) is missing or empty.");
        }
        if (!StringUtils.hasText(rawData.getCurrency())) {
            validationErrors.add("Currency (Ccy) is missing or empty.");
        }

        // Debtor information
        if (!StringUtils.hasText(rawData.getDebtorName())) {
            validationErrors.add("Debtor Name (Dbtr.Nm) is missing or empty.");
        }
        // Debtor ID is optional as per common practice, but if the PRD implies it's mandatory, add check.
        // For now, let's assume it can be optional or not always present in a simple form.
        // if (!StringUtils.hasText(rawData.getDebtorId())) {
        //     validationErrors.add("Debtor ID (Dbtr.Id) is missing or empty.");
        // }

        // Creditor information
        if (!StringUtils.hasText(rawData.getCreditorName())) {
            validationErrors.add("Creditor Name (Cdtr.Nm) is missing or empty.");
        }
        // Creditor ID is optional as per common practice.
        // if (!StringUtils.hasText(rawData.getCreditorId())) {
        //    validationErrors.add("Creditor ID (Cdtr.Id) is missing or empty.");
        // }

        // InstructionId and EndToEndId are often important but can sometimes be optional
        // depending on the specific pain.001 variant or use case.
        // For this basic validation, we'll consider them important but not strictly mandatory
        // unless explicitly stated by PRD for all cases.
        if (!StringUtils.hasText(rawData.getInstrId())) {
            validationErrors.add("InstructionId (InstrId) is missing or empty.");
        }
        if (!StringUtils.hasText(rawData.getEndToEndId())) {
            validationErrors.add("EndToEndId is missing or empty.");
        }


        if (!validationErrors.isEmpty()) {
            throw new SchemaValidationException("Schema validation failed: " + String.join(", ", validationErrors));
        }
    }
}
