package com.example.validation_service.service.rules;

import com.example.validation_service.dto.RawPaymentData;
import com.example.validation_service.service.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class CurrencyValidationRuleTest {

    private CurrencyValidationRule currencyValidationRule;
    private RawPaymentData rawPaymentData;
    private ValidationResult validationResult;

    // Default values from application.properties for testing: USD,EUR,GBP,JPY,CHF,CAD,AUD
    private final String ALLOWED_CURRENCIES_CSV = "USD,EUR,GBP,JPY,CHF,CAD,AUD";

    @BeforeEach
    void setUp() {
        currencyValidationRule = new CurrencyValidationRule(ALLOWED_CURRENCIES_CSV);
        rawPaymentData = new RawPaymentData();
        rawPaymentData.setMsgId("TestMsgId");
        validationResult = new ValidationResult();
    }

    @ParameterizedTest
    @ValueSource(strings = {"USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD"})
    void testValidate_AllowedCurrency_Success(String currency) {
        rawPaymentData.setCurrency(currency);
        currencyValidationRule.validate(rawPaymentData, validationResult);
        assertFalse(validationResult.hasErrors());
    }

    @ParameterizedTest
    @ValueSource(strings = {"usd", "eur", "gbp"}) // Test lowercase, should still pass due to toUpperCase() in rule
    void testValidate_AllowedCurrency_CaseInsensitive_Success(String currency) {
        rawPaymentData.setCurrency(currency);
        currencyValidationRule.validate(rawPaymentData, validationResult);
        assertFalse(validationResult.hasErrors());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"USDD", "EURO", "GB", "XYZ", "ABC"})
    void testValidate_UnallowedCurrency_Failure(String currency) {
        rawPaymentData.setCurrency(currency);
        currencyValidationRule.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals(1, validationResult.getErrors().size());
        assertTrue(validationResult.getErrors().get(0).contains("is not allowed. Allowed currencies are:"));
    }

    @Test
    void testValidate_NullCurrency_Failure() {
        rawPaymentData.setCurrency(null);
        currencyValidationRule.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals(1, validationResult.getErrors().size());
        assertEquals("Currency is missing.", validationResult.getErrors().get(0));
    }

    @Test
    void testValidate_EmptyCurrency_Failure() {
        rawPaymentData.setCurrency("");
        currencyValidationRule.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals(1, validationResult.getErrors().size());
        assertEquals("Currency is missing.", validationResult.getErrors().get(0));
    }
    
    @Test
    void testValidate_BlankCurrency_Failure() {
        rawPaymentData.setCurrency("   ");
        currencyValidationRule.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals(1, validationResult.getErrors().size());
        assertEquals("Currency is missing.", validationResult.getErrors().get(0));
    }

    @Test
    void testValidate_ConstructorWithEmptyCsv_UsesDefaults() {
        // This tests if the constructor fallback to defaults works, though the current test setup always provides CSV.
        // A more direct way would be to instantiate with "" and check allowedCurrencies field if it were accessible,
        // or test behavior.
        CurrencyValidationRule ruleWithEmptyCsv = new CurrencyValidationRule("");
        rawPaymentData.setCurrency("USD"); // Default set in constructor is USD, EUR, GBP
        ruleWithEmptyCsv.validate(rawPaymentData, validationResult);
        assertFalse(validationResult.hasErrors(), "Should pass for USD with default currencies");

        validationResult = new ValidationResult(); // reset
        rawPaymentData.setCurrency("JPY"); // JPY is not in the default set of the constructor
        ruleWithEmptyCsv.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors(), "Should fail for JPY with default currencies if CSV was empty");
    }
    
    @Test
    void testValidate_ConstructorWithNullCsv_UsesDefaults() {
        CurrencyValidationRule ruleWithNullCsv = new CurrencyValidationRule(null);
        rawPaymentData.setCurrency("EUR"); // Default set in constructor is USD, EUR, GBP
        ruleWithNullCsv.validate(rawPaymentData, validationResult);
        assertFalse(validationResult.hasErrors(), "Should pass for EUR with default currencies");

        validationResult = new ValidationResult(); // reset
        rawPaymentData.setCurrency("AUD"); // AUD is not in the default set of the constructor
        ruleWithNullCsv.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors(), "Should fail for AUD with default currencies if CSV was null");
    }
}
