package com.example.validation_service.service;

import com.example.validation_service.model.document.ValidationFailureLog;
import com.example.validation_service.repository.ValidationFailureLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest
public class AuditServiceIT {

    private static final String MONGO_IMAGE_NAME = "mongo:7.0";

    @Container
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse(MONGO_IMAGE_NAME));

    @Autowired
    private AuditService auditService;

    @Autowired
    private ValidationFailureLogRepository validationFailureLogRepository;
    
    @Value("${audit.payload.max-length:1000}") // Load from application.properties
    private int maxPayloadLength;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @BeforeEach
    @AfterEach
    void cleanupDatabase() {
        validationFailureLogRepository.deleteAll();
    }

    @Test
    void testLogFailure_Success_And_PayloadTruncation() {
        String messageId = "MSGID_SINGLE_001";
        String errorType = "parsing";
        String errorMessage = "Failed to parse XML.";
        String originalPayload = "A".repeat(maxPayloadLength + 100); // Payload longer than max length
        
        String expectedTruncatedPayloadSuffix = "... (truncated)";
        String expectedTruncatedPayload = originalPayload.substring(0, maxPayloadLength - expectedTruncatedPayloadSuffix.length()) 
                                          + expectedTruncatedPayloadSuffix;


        Instant beforeLog = Instant.now();
        auditService.logFailure(messageId, errorType, errorMessage, originalPayload);

        // Since AuditService.logFailure is @Async, we need to wait for the operation to complete.
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<ValidationFailureLog> logs = validationFailureLogRepository.findAll();
            assertThat(logs).hasSize(1);

            ValidationFailureLog log = logs.get(0);
            assertThat(log.getMessageId()).isEqualTo(messageId);
            assertThat(log.getErrorType()).isEqualTo(errorType);
            assertThat(log.getErrorMessage()).isEqualTo(errorMessage);
            assertThat(log.getRawPayload()).isEqualTo(expectedTruncatedPayload);
            assertThat(log.getRawPayload().length()).isEqualTo(maxPayloadLength);
            assertThat(log.getTimestamp()).isNotNull();
            assertThat(log.getTimestamp()).isAfterOrEqualTo(beforeLog.minusSeconds(1)); // Allow for slight clock differences
            assertThat(log.getTimestamp()).isBeforeOrEqualTo(Instant.now().plusSeconds(1));
        });
    }
    
    @Test
    void testLogFailure_PayloadShorterThanMaxLength_NotTruncated() {
        String messageId = "MSGID_SHORT_001";
        String errorType = "business";
        String errorMessage = "Amount too low.";
        String originalPayload = "A".repeat(maxPayloadLength - 10); // Payload shorter than max length
        
        auditService.logFailure(messageId, errorType, errorMessage, originalPayload);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<ValidationFailureLog> logs = validationFailureLogRepository.findAll();
            assertThat(logs).hasSize(1);
            ValidationFailureLog log = logs.get(0);
            assertThat(log.getMessageId()).isEqualTo(messageId);
            assertThat(log.getRawPayload()).isEqualTo(originalPayload);
            assertThat(log.getRawPayload().length()).isEqualTo(maxPayloadLength - 10);
        });
    }
    
    @Test
    void testLogFailure_PayloadEqualToMaxLength_NotTruncated() {
        String messageId = "MSGID_EQUAL_001";
        String errorType = "schema";
        String errorMessage = "Missing field.";
        String originalPayload = "A".repeat(maxPayloadLength); // Payload equal to max length
        
        auditService.logFailure(messageId, errorType, errorMessage, originalPayload);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<ValidationFailureLog> logs = validationFailureLogRepository.findAll();
            assertThat(logs).hasSize(1);
            ValidationFailureLog log = logs.get(0);
            assertThat(log.getMessageId()).isEqualTo(messageId);
            assertThat(log.getRawPayload()).isEqualTo(originalPayload);
            assertThat(log.getRawPayload().length()).isEqualTo(maxPayloadLength);
        });
    }


    @Test
    void testLogFailure_AsyncBehavior() {
        int numberOfLogs = 5;
        String baseMessageId = "MSGID_ASYNC_";
        String errorType = "business";
        String errorMessage = "Concurrent error";
        String payload = "Async test payload";

        Instant beforeLogs = Instant.now();

        for (int i = 0; i < numberOfLogs; i++) {
            auditService.logFailure(baseMessageId + i, errorType + i, errorMessage + i, payload + i);
        }

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<ValidationFailureLog> logs = validationFailureLogRepository.findAll();
            assertThat(logs).hasSize(numberOfLogs);

            // Verify data for each log entry
            List<String> loggedMessageIds = logs.stream().map(ValidationFailureLog::getMessageId).collect(Collectors.toList());
            List<String> expectedMessageIds = IntStream.range(0, numberOfLogs)
                                                       .mapToObj(i -> baseMessageId + i)
                                                       .collect(Collectors.toList());
            assertThat(loggedMessageIds).containsExactlyInAnyOrderElementsOf(expectedMessageIds);

            for (ValidationFailureLog log : logs) {
                assertThat(log.getTimestamp()).isNotNull();
                assertThat(log.getTimestamp()).isAfterOrEqualTo(beforeLogs.minusSeconds(1));
            }
        });
    }
    
    @Test
    void testLogFailure_NullPayload_HandledGracefully() {
        String messageId = "MSGID_NULL_PAYLOAD_001";
        String errorType = "system";
        String errorMessage = "Null payload encountered";
        
        auditService.logFailure(messageId, errorType, errorMessage, null);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<ValidationFailureLog> logs = validationFailureLogRepository.findAll();
            assertThat(logs).hasSize(1);
            ValidationFailureLog log = logs.get(0);
            assertThat(log.getMessageId()).isEqualTo(messageId);
            assertThat(log.getErrorType()).isEqualTo(errorType);
            assertThat(log.getErrorMessage()).isEqualTo(errorMessage);
            assertThat(log.getRawPayload()).isNull(); // Or empty string, depending on implementation; current is null
        });
    }
}
