package com.example.validation_service.service.rules;

import com.example.validation_service.dto.RawPaymentData;
import com.example.validation_service.service.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

public class AmountValidationRuleTest {

    private AmountValidationRule amountValidationRule;
    private RawPaymentData rawPaymentData;
    private ValidationResult validationResult;

    // Default values from application.properties for testing
    private final String MIN_AMOUNT = "0.01";
    private final String MAX_AMOUNT = "1000000.00";

    @BeforeEach
    void setUp() {
        amountValidationRule = new AmountValidationRule(MIN_AMOUNT, MAX_AMOUNT);
        rawPaymentData = new RawPaymentData();
        rawPaymentData.setMsgId("TestMsgId"); // For logging purposes in the rule
        validationResult = new ValidationResult();
    }

    @Test
    void testValidate_AmountInRange_Success() {
        rawPaymentData.setAmount("100.00");
        amountValidationRule.validate(rawPaymentData, validationResult);
        assertFalse(validationResult.hasErrors());
    }
    
    @Test
    void testValidate_AmountAtMinBoundary_Success() {
        rawPaymentData.setAmount(MIN_AMOUNT);
        amountValidationRule.validate(rawPaymentData, validationResult);
        assertFalse(validationResult.hasErrors());
    }

    @Test
    void testValidate_AmountAtMaxBoundary_Success() {
        rawPaymentData.setAmount(MAX_AMOUNT);
        amountValidationRule.validate(rawPaymentData, validationResult);
        assertFalse(validationResult.hasErrors());
    }

    @Test
    void testValidate_AmountBelowMin_Failure() {
        rawPaymentData.setAmount("0.00");
        amountValidationRule.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals(1, validationResult.getErrors().size());
        assertTrue(validationResult.getErrors().get(0).contains("less than minimum allowed"));
    }

    @Test
    void testValidate_AmountAboveMax_Failure() {
        rawPaymentData.setAmount("1000000.01");
        amountValidationRule.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals(1, validationResult.getErrors().size());
        assertTrue(validationResult.getErrors().get(0).contains("greater than maximum allowed"));
    }

    @Test
    void testValidate_NullAmount_Failure() {
        rawPaymentData.setAmount(null);
        amountValidationRule.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals(1, validationResult.getErrors().size());
        assertEquals("Amount is missing.", validationResult.getErrors().get(0));
    }

    @Test
    void testValidate_EmptyAmount_Failure() {
        rawPaymentData.setAmount("");
        amountValidationRule.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals(1, validationResult.getErrors().size());
        assertEquals("Amount is missing.", validationResult.getErrors().get(0));
    }
    
    @Test
    void testValidate_BlankAmount_Failure() {
        rawPaymentData.setAmount("   ");
        amountValidationRule.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals(1, validationResult.getErrors().size());
        assertEquals("Amount is missing.", validationResult.getErrors().get(0));
    }

    @ParameterizedTest
    @CsvSource({
            "abc, Invalid amount format: abc",
            "12.34.56, Invalid amount format: 12.34.56",
            "'$100', Invalid amount format: '$100'"
    })
    void testValidate_InvalidAmountFormat_Failure(String invalidAmount, String expectedErrorMessage) {
        rawPaymentData.setAmount(invalidAmount);
        amountValidationRule.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals(1, validationResult.getErrors().size());
        assertEquals(expectedErrorMessage, validationResult.getErrors().get(0));
    }
}
