package com.example.validation_service.service.rules;

import com.example.validation_service.dto.RawPaymentData;
import com.example.validation_service.service.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class ParticipantValidationRuleTest {

    private ParticipantValidationRule participantValidationRule;
    private RawPaymentData rawPaymentData;
    private ValidationResult validationResult;

    // Default values from application.properties for testing: BANK,CUST
    private final String SUPPORTED_PREFIXES_CSV = "BANK,CUST";

    @BeforeEach
    void setUp() {
        participantValidationRule = new ParticipantValidationRule(SUPPORTED_PREFIXES_CSV);
        rawPaymentData = new RawPaymentData();
        rawPaymentData.setMsgId("TestMsgId");
        validationResult = new ValidationResult();
    }

    @ParameterizedTest
    @ValueSource(strings = {"BANK12345", "CUST007", "BANKABC", "CUSTXYZ"})
    void testValidate_SupportedParticipantPrefix_Success(String participantId) {
        rawPaymentData.setDebtorId(participantId);
        rawPaymentData.setCreditorId(participantId); // Test both fields with valid IDs
        participantValidationRule.validate(rawPaymentData, validationResult);
        assertFalse(validationResult.hasErrors());
    }

    @Test
    void testValidate_SupportedDebtorPrefix_UnsupportedCreditorPrefix_Failure() {
        rawPaymentData.setDebtorId("BANK123");
        rawPaymentData.setCreditorId("INVALIDPREFIX789");
        participantValidationRule.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals(1, validationResult.getErrors().size());
        assertTrue(validationResult.getErrors().get(0).contains("Creditor ID 'INVALIDPREFIX789' does not start with a supported prefix."));
    }
    
    @Test
    void testValidate_UnsupportedDebtorPrefix_SupportedCreditorPrefix_Failure() {
        rawPaymentData.setDebtorId("INVALIDPREFIX789");
        rawPaymentData.setCreditorId("BANK123");
        participantValidationRule.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals(1, validationResult.getErrors().size());
        assertTrue(validationResult.getErrors().get(0).contains("Debtor ID 'INVALIDPREFIX789' does not start with a supported prefix."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWNID", "CLIENT001", "VENDOR777"})
    void testValidate_UnsupportedParticipantPrefix_Failure(String participantId) {
        rawPaymentData.setDebtorId(participantId);
        rawPaymentData.setCreditorId("BANKVALID"); // Keep one valid to isolate the failing one
        
        participantValidationRule.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals(1, validationResult.getErrors().size());
        assertTrue(validationResult.getErrors().get(0).contains("Debtor ID '" + participantId + "' does not start with a supported prefix."));

        // Reset and test for Creditor ID
        validationResult = new ValidationResult();
        rawPaymentData.setDebtorId("CUSTVALID");
        rawPaymentData.setCreditorId(participantId);
        participantValidationRule.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals(1, validationResult.getErrors().size());
        assertTrue(validationResult.getErrors().get(0).contains("Creditor ID '" + participantId + "' does not start with a supported prefix."));
    }

    @Test
    void testValidate_NullParticipantIds_Success_AsPerCurrentRuleLogic() {
        // Rule currently allows null/empty IDs without erroring
        rawPaymentData.setDebtorId(null);
        rawPaymentData.setCreditorId(null);
        participantValidationRule.validate(rawPaymentData, validationResult);
        assertFalse(validationResult.hasErrors());
    }

    @Test
    void testValidate_EmptyParticipantIds_Success_AsPerCurrentRuleLogic() {
        rawPaymentData.setDebtorId("");
        rawPaymentData.setCreditorId("   "); // Blank is also treated as empty by StringUtils.hasText
        participantValidationRule.validate(rawPaymentData, validationResult);
        assertFalse(validationResult.hasErrors());
    }
    
    @Test
    void testValidate_MixedValidAndNullIds_Success() {
        rawPaymentData.setDebtorId("BANK123");
        rawPaymentData.setCreditorId(null);
        participantValidationRule.validate(rawPaymentData, validationResult);
        assertFalse(validationResult.hasErrors());

        validationResult = new ValidationResult();
        rawPaymentData.setDebtorId(null);
        rawPaymentData.setCreditorId("CUSTXYZ");
        participantValidationRule.validate(rawPaymentData, validationResult);
        assertFalse(validationResult.hasErrors());
    }

    @Test
    void testValidate_ConstructorWithEmptyCsv_UsesDefaults() {
        ParticipantValidationRule ruleWithEmptyCsv = new ParticipantValidationRule("");
        // Default prefixes are BANK, CUST
        rawPaymentData.setDebtorId("BANKdefault");
        ruleWithEmptyCsv.validate(rawPaymentData, validationResult);
        assertFalse(validationResult.hasErrors());

        validationResult = new ValidationResult();
        rawPaymentData.setDebtorId("OTHERdefault"); // Not BANK or CUST
        ruleWithEmptyCsv.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors());
    }
}
