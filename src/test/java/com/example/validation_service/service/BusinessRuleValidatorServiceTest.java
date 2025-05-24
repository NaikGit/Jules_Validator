package com.example.validation_service.service;

import com.example.validation_service.dto.RawPaymentData;
import com.example.validation_service.service.rules.ValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BusinessRuleValidatorServiceTest {

    @Mock
    private ValidationRule rule1;

    @Mock
    private ValidationRule rule2;
    
    @Mock
    private ValidationRule ruleWithError;


    private BusinessRuleValidatorService businessRuleValidatorService;
    private RawPaymentData rawPaymentData;

    @BeforeEach
    void setUp() {
        rawPaymentData = new RawPaymentData(); // Initialize with test data if needed by rules
        rawPaymentData.setMsgId("TestMsgId");
    }

    @Test
    void testValidate_AllRulesPass_NoException() {
        List<ValidationRule> rules = Arrays.asList(rule1, rule2);
        businessRuleValidatorService = new BusinessRuleValidatorService(rules);

        // Configure mocks for rule1 and rule2 to not add errors
        doAnswer(invocation -> {
            // ValidationResult result = invocation.getArgument(1);
            // No errors added
            return null;
        }).when(rule1).validate(any(RawPaymentData.class), any(ValidationResult.class));

        doAnswer(invocation -> {
            // ValidationResult result = invocation.getArgument(1);
            // No errors added
            return null;
        }).when(rule2).validate(any(RawPaymentData.class), any(ValidationResult.class));

        assertDoesNotThrow(() -> businessRuleValidatorService.validate(rawPaymentData));

        verify(rule1, times(1)).validate(eq(rawPaymentData), any(ValidationResult.class));
        verify(rule2, times(1)).validate(eq(rawPaymentData), any(ValidationResult.class));
    }

    @Test
    void testValidate_OneRuleFails_ThrowsBusinessValidationExceptionWithErrors() {
        List<ValidationRule> rules = Arrays.asList(rule1, rule2);
        businessRuleValidatorService = new BusinessRuleValidatorService(rules);

        // Rule1 passes
        doNothing().when(rule1).validate(any(RawPaymentData.class), any(ValidationResult.class));
        
        // Rule2 fails
        String rule2ErrorMessage = "Error from rule 2";
        doAnswer(invocation -> {
            ValidationResult result = invocation.getArgument(1);
            result.addError(rule2ErrorMessage);
            return null;
        }).when(rule2).validate(any(RawPaymentData.class), any(ValidationResult.class));


        BusinessValidationException exception = assertThrows(BusinessValidationException.class, () -> {
            businessRuleValidatorService.validate(rawPaymentData);
        });

        assertEquals(1, exception.getValidationErrors().size());
        assertTrue(exception.getValidationErrors().contains(rule2ErrorMessage));

        verify(rule1, times(1)).validate(eq(rawPaymentData), any(ValidationResult.class));
        verify(rule2, times(1)).validate(eq(rawPaymentData), any(ValidationResult.class));
    }

    @Test
    void testValidate_MultipleRulesFail_ThrowsBusinessValidationExceptionWithAllErrors() {
        List<ValidationRule> rules = Arrays.asList(rule1, rule2);
        businessRuleValidatorService = new BusinessRuleValidatorService(rules);

        String rule1ErrorMessage = "Error from rule 1";
        doAnswer(invocation -> {
            ValidationResult result = invocation.getArgument(1);
            result.addError(rule1ErrorMessage);
            return null;
        }).when(rule1).validate(any(RawPaymentData.class), any(ValidationResult.class));
        
        String rule2ErrorMessage = "Error from rule 2";
        doAnswer(invocation -> {
            ValidationResult result = invocation.getArgument(1);
            result.addError(rule2ErrorMessage);
            return null;
        }).when(rule2).validate(any(RawPaymentData.class), any(ValidationResult.class));

        BusinessValidationException exception = assertThrows(BusinessValidationException.class, () -> {
            businessRuleValidatorService.validate(rawPaymentData);
        });

        assertEquals(2, exception.getValidationErrors().size());
        assertTrue(exception.getValidationErrors().contains(rule1ErrorMessage));
        assertTrue(exception.getValidationErrors().contains(rule2ErrorMessage));
    }

    @Test
    void testValidate_RuleThrowsUnexpectedException_AddsSystemErrorAndContinues() {
         List<ValidationRule> rules = Arrays.asList(ruleWithError, rule1); // ruleWithError first
        businessRuleValidatorService = new BusinessRuleValidatorService(rules);

        String unexpectedExceptionMessage = "Unexpected runtime error in ruleWithError";
        doThrow(new RuntimeException(unexpectedExceptionMessage))
            .when(ruleWithError).validate(any(RawPaymentData.class), any(ValidationResult.class));

        // rule1 passes
        doNothing().when(rule1).validate(any(RawPaymentData.class), any(ValidationResult.class));
        
        BusinessValidationException exception = assertThrows(BusinessValidationException.class, () -> {
            businessRuleValidatorService.validate(rawPaymentData);
        });

        assertEquals(1, exception.getValidationErrors().size());
        String systemError = exception.getValidationErrors().get(0);
        assertTrue(systemError.contains("System error during validation rule"));
        assertTrue(systemError.contains(ruleWithError.getClass().getSimpleName())); // Or a mocked name
        assertTrue(systemError.contains(unexpectedExceptionMessage));
        
        verify(ruleWithError, times(1)).validate(eq(rawPaymentData), any(ValidationResult.class));
        verify(rule1, times(1)).validate(eq(rawPaymentData), any(ValidationResult.class)); // Ensure other rules are still processed
    }
    
    @Test
    void testValidate_NoRules_NoException() {
        businessRuleValidatorService = new BusinessRuleValidatorService(Collections.emptyList());
        assertDoesNotThrow(() -> businessRuleValidatorService.validate(rawPaymentData));
    }
}
