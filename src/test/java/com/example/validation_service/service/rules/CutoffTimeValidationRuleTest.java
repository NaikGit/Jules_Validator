package com.example.validation_service.service.rules;

import com.example.validation_service.dto.RawPaymentData;
import com.example.validation_service.service.ValidationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class CutoffTimeValidationRuleTest {

    private CutoffTimeValidationRule cutoffTimeValidationRule;
    private RawPaymentData rawPaymentData;
    private ValidationResult validationResult;
    private MockedStatic<LocalTime> mockedLocalTime;
    private MockedStatic<ZoneId> mockedZoneId;


    // Default from application.properties "17:00:00"
    private final String CUTOFF_TIME_STR = "17:00:00";
    private final LocalTime CUTOFF_TIME = LocalTime.parse(CUTOFF_TIME_STR);
    private final ZoneId SYSTEM_ZONE = ZoneId.systemDefault(); // As used in the rule

    @BeforeEach
    void setUp() {
        cutoffTimeValidationRule = new CutoffTimeValidationRule(CUTOFF_TIME_STR);
        rawPaymentData = new RawPaymentData();
        rawPaymentData.setMsgId("TestMsgId");
        validationResult = new ValidationResult();

        // Mock ZoneId.systemDefault() to ensure consistent zone for tests
        mockedZoneId = Mockito.mockStatic(ZoneId.class, Mockito.CALLS_REAL_METHODS);
        when(ZoneId.systemDefault()).thenReturn(SYSTEM_ZONE);


        // Mock LocalTime.now(ZoneId)
        // It's important to mock LocalTime.now(ZoneId) rather than LocalTime.now() if the rule specifies a zone.
        // The rule uses LocalTime.now(systemZoneId)
        mockedLocalTime = Mockito.mockStatic(LocalTime.class, Mockito.CALLS_REAL_METHODS);

    }

    @AfterEach
    void tearDown() {
        if (mockedLocalTime != null) {
            mockedLocalTime.close();
        }
        if (mockedZoneId != null) {
            mockedZoneId.close();
        }
    }

    @Test
    void testValidate_BeforeCutoff_Success() {
        LocalTime timeBeforeCutoff = CUTOFF_TIME.minusHours(1);
        mockedLocalTime.when(() -> LocalTime.now(SYSTEM_ZONE)).thenReturn(timeBeforeCutoff);

        cutoffTimeValidationRule.validate(rawPaymentData, validationResult);
        assertFalse(validationResult.hasErrors());
    }

    @Test
    void testValidate_AtCutoff_Success() {
        // Current implementation means "after cutoff" fails. So, at cutoff should pass.
        LocalTime timeAtCutoff = CUTOFF_TIME;
        mockedLocalTime.when(() -> LocalTime.now(SYSTEM_ZONE)).thenReturn(timeAtCutoff);

        cutoffTimeValidationRule.validate(rawPaymentData, validationResult);
        assertFalse(validationResult.hasErrors());
    }
    
    @Test
    void testValidate_JustBeforeCutoff_Success() {
        LocalTime timeJustBeforeCutoff = CUTOFF_TIME.minusSeconds(1);
        mockedLocalTime.when(() -> LocalTime.now(SYSTEM_ZONE)).thenReturn(timeJustBeforeCutoff);

        cutoffTimeValidationRule.validate(rawPaymentData, validationResult);
        assertFalse(validationResult.hasErrors());
    }


    @Test
    void testValidate_AfterCutoff_Failure() {
        LocalTime timeAfterCutoff = CUTOFF_TIME.plusHours(1);
        mockedLocalTime.when(() -> LocalTime.now(SYSTEM_ZONE)).thenReturn(timeAfterCutoff);

        cutoffTimeValidationRule.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals(1, validationResult.getErrors().size());
        assertTrue(validationResult.getErrors().get(0).contains("is after the cutoff time of " + CUTOFF_TIME_STR));
    }
    
    @Test
    void testValidate_JustAfterCutoff_Failure() {
        LocalTime timeJustAfterCutoff = CUTOFF_TIME.plusSeconds(1);
         mockedLocalTime.when(() -> LocalTime.now(SYSTEM_ZONE)).thenReturn(timeJustAfterCutoff);

        cutoffTimeValidationRule.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals(1, validationResult.getErrors().size());
        assertTrue(validationResult.getErrors().get(0).contains("is after the cutoff time of " + CUTOFF_TIME_STR));
    }

    @Test
    void testConstructor_InvalidCutoffTimeFormat_DefaultsAndLogs() {
        // This test relies on checking the log output, which is harder to assert directly in unit tests
        // without log capture utilities. Here, we'll just check if it defaults correctly.
        // The rule logs an error and defaults to 17:00:00 if format is invalid.
        CutoffTimeValidationRule ruleWithInvalidFormat = new CutoffTimeValidationRule("INVALID-TIME");
        
        LocalTime defaultCutoff = LocalTime.of(17,0,0);
        LocalTime timeAfterDefaultCutoff = defaultCutoff.plusMinutes(1);

        mockedLocalTime.when(() -> LocalTime.now(SYSTEM_ZONE)).thenReturn(timeAfterDefaultCutoff);
        
        ruleWithInvalidFormat.validate(rawPaymentData, validationResult);
        assertTrue(validationResult.hasErrors(), "Should fail as time is after the default cutoff");
        assertTrue(validationResult.getErrors().get(0).contains("is after the cutoff time of 17:00:00"));
    }
}
