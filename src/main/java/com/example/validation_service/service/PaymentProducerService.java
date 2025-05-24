package com.example.validation_service.service;

import com.example.validation_service.dto.ValidatedPayment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class PaymentProducerService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProducerService.class);

    private final KafkaTemplate<String, ValidatedPayment> kafkaTemplate;
    private final String validatedTopicName;

    @Autowired
    public PaymentProducerService(KafkaTemplate<String, ValidatedPayment> kafkaTemplate,
                                  @Value("${app.kafka.topic.instant-payment-validated}") String validatedTopicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.validatedTopicName = validatedTopicName;
    }

    public void sendValidatedPayment(ValidatedPayment payment) {
        if (payment == null) {
            logger.warn("Cannot send null payment object.");
            return;
        }

        String key = payment.getTransactionId(); // Using transactionId as the Kafka message key

        CompletableFuture<SendResult<String, ValidatedPayment>> future =
                kafkaTemplate.send(validatedTopicName, key, payment);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Successfully sent ValidatedPayment with key {} to topic {}: Offset = {}",
                        key, validatedTopicName, result.getRecordMetadata().offset());
            } else {
                logger.error("Failed to send ValidatedPayment with key {} to topic {}: {}",
                        key, validatedTopicName, ex.getMessage(), ex);
                // Additional error handling could be implemented here, e.g.,
                // - Storing the failed message for retry
                // - Sending to a dead-letter topic (DLT)
            }
        });
    }
}
