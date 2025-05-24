package com.example.validation_service.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.validation_service.service.PaymentProcessingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentMessageConsumer.class);

    private final PaymentProcessingService paymentProcessingService;

    @Autowired
    public PaymentMessageConsumer(PaymentProcessingService paymentProcessingService) {
        this.paymentProcessingService = paymentProcessingService;
    }

    @KafkaListener(topics = "${app.kafka.topic.instant-payment-inbound}",
                   groupId = "${spring.kafka.consumer.group-id}")
    public void receivePaymentMessage(ConsumerRecord<String, String> record) {
        String xmlPayload = record.value();
        String kafkaKey = record.key(); // Potentially useful for logging context
        long offset = record.offset();
        int partition = record.partition();

        logger.info("Received message: key='{}', partition={}, offset={}, topic='{}'",
                    kafkaKey, partition, offset, record.topic());
        logger.debug("Payload: {}", xmlPayload);


        if (xmlPayload == null || xmlPayload.trim().isEmpty()) {
            logger.warn("Received null or empty message from Kafka. Key: {}. Skipping processing.", kafkaKey);
            // Optionally, send to a dead-letter topic or log to audit service if this is unexpected.
            // For now, just logging.
            return;
        }

        try {
            paymentProcessingService.process(xmlPayload);
        } catch (Exception e) {
            // This catch block is for unexpected errors propagating from PaymentProcessingService
            // or issues not caught within its own try-catch (which should be rare if comprehensive).
            // PaymentProcessingService is designed to handle its own exceptions and log them.
            // This is a last resort.
            logger.error("Unhandled exception during message processing for key {}. Payload: {}. Error: {}",
                         kafkaKey, xmlPayload, e.getMessage(), e);
            // Depending on requirements, might still call auditService here with a generic "consumer_error"
            // However, PaymentProcessingService should have already logged the specific failure.
        }
    }
}
