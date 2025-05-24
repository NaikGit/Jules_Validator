package com.example.validation_service.service;

import com.example.validation_service.model.document.ValidationFailureLog;
import com.example.validation_service.repository.ValidationFailureLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final ValidationFailureLogRepository failureLogRepository;
    private final int maxPayloadLength;

    @Autowired
    public AuditService(ValidationFailureLogRepository failureLogRepository,
                        @Value("${audit.payload.max-length:1000}") int maxPayloadLength) {
        this.failureLogRepository = failureLogRepository;
        this.maxPayloadLength = maxPayloadLength;
        logger.info("AuditService initialized with maxPayloadLength: {}", maxPayloadLength);
    }

    @Async
    public void logFailure(String messageId, String errorType, String errorMessage, String rawPayload) {
        logger.debug("Attempting to log failure for messageId: {}", messageId);
        try {
            String processedPayload = rawPayload;
            if (rawPayload != null && rawPayload.length() > maxPayloadLength) {
                processedPayload = rawPayload.substring(0, maxPayloadLength) + "... (truncated)";
                logger.warn("Raw payload for messageId {} was truncated from {} to {} characters.",
                            messageId, rawPayload.length(), maxPayloadLength);
            }

            ValidationFailureLog failureLog = new ValidationFailureLog(
                    messageId,
                    errorType,
                    errorMessage,
                    processedPayload
            );
            failureLogRepository.save(failureLog);
            logger.info("Successfully logged failure for messageId {} of type {}", messageId, errorType);
        } catch (Exception e) {
            // Log the exception that occurred during the async operation
            // Avoid throwing exceptions from @Async methods if they are not handled by an AsyncUncaughtExceptionHandler
            logger.error("Failed to log failure to MongoDB for messageId {}: {}", messageId, e.getMessage(), e);
        }
    }

    // Placeholder for logging successful events if needed in the future
    /*
    @Async
    public void logSuccess(String messageId, String operation, String details) {
        // Implementation for logging successful events, potentially to a different collection or format
        logger.info("Successfully processed messageId {}: Operation - {}, Details - {}", messageId, operation, details);
        // Example: SuccessLog successLog = new SuccessLog(messageId, operation, details, Instant.now());
        // successLogRepository.save(successLog);
    }
    */
}
