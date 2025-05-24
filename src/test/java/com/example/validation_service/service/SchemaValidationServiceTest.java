package com.example.validation_service.service;

import com.example.validation_service.dto.RawPaymentData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class SchemaValidationServiceTest {

    private SchemaValidationService schemaValidationService;

    @BeforeEach
    void setUp() {
        schemaValidationService = new SchemaValidationService();
    }

    private RawPaymentData createValidRawPaymentData() {
        RawPaymentData data = new RawPaymentData();
        data.setMsgId("MSG001");
        data.setInstrId("INSTR001");
        data.setEndToEndId("ENDTOEND001");
        data.setDebtorName("Debtor Name");
        data.setDebtorId("DEBTORID"); // Optional in terms of schema validation failing, but good to have
        data.setCreditorName("Creditor Name");
        data.setCreditorId("CREDITORID"); // Optional
        data.setAmount("100.00");
        data.setCurrency("USD");
        return data;
    }

    @Test
    void testValidate_Success() {
        RawPaymentData data = createValidRawPaymentData();
        assertDoesNotThrow(() -> schemaValidationService.validate(data));
    }

    @Test
    void testValidate_NullRawPaymentData_ThrowsSchemaValidationException() {
        SchemaValidationException exception = assertThrows(SchemaValidationException.class, () -> {
            schemaValidationService.validate(null);
        });
        assertEquals("RawPaymentData object cannot be null.", exception.getMessage());
    }
    
    // Parameterized test for missing or blank fields
    // Each method in the stream represents a way to make RawPaymentData invalid
    static Stream<Consumer<RawPaymentData>> invalidDataProvider() {
        return Stream.of(
                data -> data.setMsgId(null),
                data -> data.setMsgId(""),
                data -> data.setMsgId("   "),
                data -> data.setAmount(null),
                data -> data.setAmount(""),
                data -> data.setCurrency(null),
                data -> data.setCurrency("  "),
                data -> data.setDebtorName(null),
                data -> data.setDebtorName(""),
                data -> data.setCreditorName(null),
                data -> data.setCreditorName(""),
                data -> data.setInstrId(null), // Added for InstrId
                data -> data.setInstrId(""),     // Added for InstrId
                data -> data.setEndToEndId(null), // Added for EndToEndId
                data -> data.setEndToEndId("")    // Added for EndToEndId
        );
    }

    @ParameterizedTest
    @MethodSource("invalidDataProvider")
    void testValidate_MissingOrBlankFields_ThrowsSchemaValidationException(Consumer<RawPaymentData> invalidator) {
        RawPaymentData data = createValidRawPaymentData();
        invalidator.accept(data); // Apply the invalidating change

        SchemaValidationException exception = assertThrows(SchemaValidationException.class, () -> {
            schemaValidationService.validate(data);
        });
        assertTrue(exception.getMessage().startsWith("Schema validation failed:"));
        
        // Check if the specific error message is present
        if (data.getMsgId() == null || data.getMsgId().trim().isEmpty()) {
            assertTrue(exception.getMessage().contains("MsgId is missing or empty."));
        }
        if (data.getAmount() == null || data.getAmount().trim().isEmpty()) {
            assertTrue(exception.getMessage().contains("Amount (Amt) is missing or empty."));
        }
        if (data.getCurrency() == null || data.getCurrency().trim().isEmpty()) {
            assertTrue(exception.getMessage().contains("Currency (Ccy) is missing or empty."));
        }
        if (data.getDebtorName() == null || data.getDebtorName().trim().isEmpty()) {
            assertTrue(exception.getMessage().contains("Debtor Name (Dbtr.Nm) is missing or empty."));
        }
        if (data.getCreditorName() == null || data.getCreditorName().trim().isEmpty()) {
            assertTrue(exception.getMessage().contains("Creditor Name (Cdtr.Nm) is missing or empty."));
        }
        if (data.getInstrId() == null || data.getInstrId().trim().isEmpty()) {
            assertTrue(exception.getMessage().contains("InstructionId (InstrId) is missing or empty."));
        }
        if (data.getEndToEndId() == null || data.getEndToEndId().trim().isEmpty()) {
            assertTrue(exception.getMessage().contains("EndToEndId is missing or empty."));
        }
    }
}
