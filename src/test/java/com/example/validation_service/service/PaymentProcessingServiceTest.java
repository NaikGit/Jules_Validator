package com.example.validation_service.service;

import com.example.validation_service.dto.RawPaymentData;
import com.example.validation_service.dto.ValidatedPayment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentProcessingServiceTest {

    @Mock
    private XmlParserService xmlParserService;
    @Mock
    private SchemaValidationService schemaValidationService;
    @Mock
    private BusinessRuleValidatorService businessRuleValidatorService;
    @Mock
    private PaymentTransformerService paymentTransformerService;
    @Mock
    private PaymentProducerService paymentProducerService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private PaymentProcessingService paymentProcessingService;

    private RawPaymentData mockRawPaymentData;
    private ValidatedPayment mockValidatedPayment;
    private final String testXmlPayload = "<xml>test</xml>";
    private final String testMsgId = "TestMsgId123";

    @BeforeEach
    void setUp() {
        mockRawPaymentData = new RawPaymentData();
        mockRawPaymentData.setMsgId(testMsgId); 
        // Populate other fields if necessary for specific tests, though not strictly needed if methods are mocked

        mockValidatedPayment = new ValidatedPayment(); // Populate if needed
    }

    @Test
    void testProcess_Success() throws Exception {
        // Arrange
        when(xmlParserService.parse(anyString())).thenReturn(mockRawPaymentData);
        doNothing().when(schemaValidationService).validate(any(RawPaymentData.class));
        doNothing().when(businessRuleValidatorService).validate(any(RawPaymentData.class));
        when(paymentTransformerService.transform(any(RawPaymentData.class))).thenReturn(mockValidatedPayment);
        doNothing().when(paymentProducerService).sendValidatedPayment(any(ValidatedPayment.class));

        // Act
        paymentProcessingService.process(testXmlPayload);

        // Assert
        verify(xmlParserService).parse(testXmlPayload);
        verify(schemaValidationService).validate(mockRawPaymentData);
        verify(businessRuleValidatorService).validate(mockRawPaymentData);
        verify(paymentTransformerService).transform(mockRawPaymentData);
        verify(paymentProducerService).sendValidatedPayment(mockValidatedPayment);
        verify(auditService, never()).logFailure(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testProcess_XmlParsingException_LogsFailure() throws Exception {
        // Arrange
        XmlParsingException exception = new XmlParsingException("XML parsing failed");
        when(xmlParserService.parse(anyString())).thenThrow(exception);

        // Act
        paymentProcessingService.process(testXmlPayload);

        // Assert
        verify(xmlParserService).parse(testXmlPayload);
        // The current extractMessageIdSafe returns null if parsing fails early before MsgId is extracted.
        verify(auditService).logFailure(eq(null), eq("parsing"), eq("XML parsing failed"), eq(testXmlPayload));
        verifyNoInteractions(schemaValidationService, businessRuleValidatorService, paymentTransformerService, paymentProducerService);
    }
    
    @Test
    void testProcess_XmlParsingException_LogsFailure_MsgIdFromSafeExtract() throws Exception {
        // This test assumes extractMessageIdSafe can somehow get an ID, 
        // current impl of extractMessageIdSafe returns null for XmlParsingException.
        // To make this test meaningful, we'd need to change how extractMessageIdSafe works or how XmlParsingException stores data.
        // For now, this test will behave like the one above. If extractMessageIdSafe is improved, this test would demonstrate it.
        String specificMsgIdInPayload = "SPECIFIC_MSG_ID";
        String payloadWithSpecificMsgId = String.format("<Doc><GrpHdr><MsgId>%s</MsgId></GrpHdr>...</Doc>", specificMsgIdInPayload);

        XmlParsingException exception = new XmlParsingException("XML parsing failed on specific");
        when(xmlParserService.parse(eq(payloadWithSpecificMsgId))).thenThrow(exception);
        // If extractMessageIdSafe were to use regex, it might find specificMsgIdInPayload
        // For the current implementation, it will be null.
        
        // Act
        paymentProcessingService.process(payloadWithSpecificMsgId);

        // Assert
        verify(auditService).logFailure(eq(null), eq("parsing"), eq("XML parsing failed on specific"), eq(payloadWithSpecificMsgId));
    }


    @Test
    void testProcess_SchemaValidationException_LogsFailure() throws Exception {
        // Arrange
        when(xmlParserService.parse(anyString())).thenReturn(mockRawPaymentData);
        SchemaValidationException exception = new SchemaValidationException("Schema validation failed");
        doThrow(exception).when(schemaValidationService).validate(any(RawPaymentData.class));

        // Act
        paymentProcessingService.process(testXmlPayload);

        // Assert
        verify(xmlParserService).parse(testXmlPayload);
        verify(schemaValidationService).validate(mockRawPaymentData);
        verify(auditService).logFailure(eq(testMsgId), eq("schema"), eq("Schema validation failed"), eq(testXmlPayload));
        verifyNoInteractions(businessRuleValidatorService, paymentTransformerService, paymentProducerService);
    }

    @Test
    void testProcess_BusinessValidationException_LogsFailure() throws Exception {
        // Arrange
        when(xmlParserService.parse(anyString())).thenReturn(mockRawPaymentData);
        doNothing().when(schemaValidationService).validate(any(RawPaymentData.class));
        BusinessValidationException exception = new BusinessValidationException("Business validation failed", Collections.singletonList("Rule X failed"));
        doThrow(exception).when(businessRuleValidatorService).validate(any(RawPaymentData.class));

        // Act
        paymentProcessingService.process(testXmlPayload);

        // Assert
        verify(xmlParserService).parse(testXmlPayload);
        verify(schemaValidationService).validate(mockRawPaymentData);
        verify(businessRuleValidatorService).validate(mockRawPaymentData);
        verify(auditService).logFailure(eq(testMsgId), eq("business"), eq("Rule X failed"), eq(testXmlPayload));
        verifyNoInteractions(paymentTransformerService, paymentProducerService);
    }

    @Test
    void testProcess_TransformationException_LogsFailure() throws Exception {
        // Arrange
        when(xmlParserService.parse(anyString())).thenReturn(mockRawPaymentData);
        doNothing().when(schemaValidationService).validate(any(RawPaymentData.class));
        doNothing().when(businessRuleValidatorService).validate(any(RawPaymentData.class));
        TransformationException exception = new TransformationException("Transformation failed");
        when(paymentTransformerService.transform(any(RawPaymentData.class))).thenThrow(exception);

        // Act
        paymentProcessingService.process(testXmlPayload);

        // Assert
        verify(xmlParserService).parse(testXmlPayload);
        verify(schemaValidationService).validate(mockRawPaymentData);
        verify(businessRuleValidatorService).validate(mockRawPaymentData);
        verify(paymentTransformerService).transform(mockRawPaymentData);
        verify(auditService).logFailure(eq(testMsgId), eq("transformation"), eq("Transformation failed"), eq(testXmlPayload));
        verifyNoInteractions(paymentProducerService);
    }

    @Test
    void testProcess_UnexpectedException_LogsFailure() throws Exception {
        // Arrange
        when(xmlParserService.parse(anyString())).thenReturn(mockRawPaymentData);
        doNothing().when(schemaValidationService).validate(any(RawPaymentData.class));
        doNothing().when(businessRuleValidatorService).validate(any(RawPaymentData.class));
        RuntimeException exception = new RuntimeException("Unexpected error");
        when(paymentTransformerService.transform(any(RawPaymentData.class))).thenThrow(exception); // Example: error in transformation

        // Act
        paymentProcessingService.process(testXmlPayload);

        // Assert
        verify(xmlParserService).parse(testXmlPayload);
        verify(schemaValidationService).validate(mockRawPaymentData);
        verify(businessRuleValidatorService).validate(mockRawPaymentData);
        verify(paymentTransformerService).transform(mockRawPaymentData);
        verify(auditService).logFailure(eq(testMsgId), eq("unknown_processing_error"), eq("Unexpected error"), eq(testXmlPayload));
        verifyNoInteractions(paymentProducerService);
    }
}
