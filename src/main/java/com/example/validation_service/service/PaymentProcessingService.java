package com.example.validation_service.service;

import com.example.validation_service.dto.RawPaymentData;
import com.example.validation_service.dto.ValidatedPayment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessingService.class);

    private final XmlParserService xmlParserService;
    private final SchemaValidationService schemaValidationService;
    private final BusinessRuleValidatorService businessRuleValidatorService;
    private final PaymentTransformerService paymentTransformerService;
    private final PaymentProducerService paymentProducerService;
    private final AuditService auditService;

    @Autowired
    public PaymentProcessingService(XmlParserService xmlParserService,
                                    SchemaValidationService schemaValidationService,
                                    BusinessRuleValidatorService businessRuleValidatorService,
                                    PaymentTransformerService paymentTransformerService,
                                    PaymentProducerService paymentProducerService,
                                    AuditService auditService) {
        this.xmlParserService = xmlParserService;
        this.schemaValidationService = schemaValidationService;
        this.businessRuleValidatorService = businessRuleValidatorService;
        this.paymentTransformerService = paymentTransformerService;
        this.paymentProducerService = paymentProducerService;
        this.auditService = auditService;
    }

    public void process(String xmlPayload) {
        RawPaymentData rawPaymentData = null;
        String messageIdForAudit = null;

        try {
            logger.info("Starting processing of XML payload.");
            rawPaymentData = xmlParserService.parse(xmlPayload);
            messageIdForAudit = rawPaymentData.getMsgId(); // Get MsgId as soon as it's available
            logger.info("Successfully parsed XML for MsgId: {}", messageIdForAudit);

            schemaValidationService.validate(rawPaymentData);
            logger.info("Successfully schema-validated data for MsgId: {}", messageIdForAudit);

            businessRuleValidatorService.validate(rawPaymentData);
            logger.info("Successfully business-rules-validated data for MsgId: {}", messageIdForAudit);

            ValidatedPayment validatedPayment = paymentTransformerService.transform(rawPaymentData);
            logger.info("Successfully transformed data for MsgId: {}", messageIdForAudit);

            paymentProducerService.sendValidatedPayment(validatedPayment);
            logger.info("Successfully sent validated payment to Kafka for MsgId: {}", messageIdForAudit);

        } catch (XmlParsingException e) {
            messageIdForAudit = extractMessageIdSafe(xmlPayload, e); // Attempt to get MsgId even on parsing failure
            logger.error("XML Parsing Exception for MsgId {}: {}", messageIdForAudit, e.getMessage(), e);
            auditService.logFailure(messageIdForAudit, "parsing", e.getMessage(), xmlPayload);
        } catch (SchemaValidationException e) {
            logger.error("Schema Validation Exception for MsgId {}: {}", messageIdForAudit, e.getMessage(), e);
            auditService.logFailure(messageIdForAudit, "schema", e.getMessage(), xmlPayload);
        } catch (BusinessValidationException e) {
            logger.error("Business Validation Exception for MsgId {}: {}", messageIdForAudit, e.getMessage(), e);
            // For BusinessValidationException, e.getValidationErrors() might be more detailed
            String errorMessage = String.join(", ", e.getValidationErrors());
            auditService.logFailure(messageIdForAudit, "business", errorMessage, xmlPayload);
        } catch (TransformationException e) {
            logger.error("Transformation Exception for MsgId {}: {}", messageIdForAudit, e.getMessage(), e);
            auditService.logFailure(messageIdForAudit, "transformation", e.getMessage(), xmlPayload);
        } catch (Exception e) {
            // Catch-all for any other unexpected exceptions during processing
            logger.error("Unexpected processing error for MsgId {}: {}", messageIdForAudit, e.getMessage(), e);
            auditService.logFailure(messageIdForAudit, "unknown_processing_error", e.getMessage(), xmlPayload);
        }
    }

    /**
     * Attempts to extract a message ID for logging even if full parsing fails.
     * This is a simplified example. A more robust solution might involve more sophisticated partial parsing
     * or looking for a specific tag if XmlParsingException doesn't offer partial data.
     * For now, it checks if the exception carries any partial data (which it currently does not).
     * If not, it returns a placeholder or null.
     *
     * @param xmlPayload The original XML payload.
     * @param ex The XmlParsingException, which might contain partial data in a future implementation.
     * @return Extracted message ID or a placeholder.
     */
    private String extractMessageIdSafe(String xmlPayload, XmlParsingException ex) {
        // Note: The current XmlParserService and XmlParsingException do not explicitly provide partial data.
        // This method is a placeholder for a more advanced implementation if needed.
        // A simple strategy could be a regex if MsgId format is very standard and early in the XML.
        // For now, as XmlParsingException doesn't carry partial data, we'll return null or a placeholder.
        // Let's assume for now if parsing fails very early, msgId might not be available.
        
        // Example of a regex approach (very basic, might not be robust enough for all XMLs):
        // Pattern pattern = Pattern.compile("<MsgId>([^<]+)</MsgId>");
        // Matcher matcher = pattern.matcher(xmlPayload);
        // if (matcher.find()) {
        //     return matcher.group(1);
        // }
        logger.warn("Full XML parsing failed. Message ID extraction for audit might be unreliable. Exception: {}", ex.getMessage());
        return null; // Or a default like "UNKNOWN_MSG_ID_DUE_TO_PARSING_FAILURE"
    }
}
