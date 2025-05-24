package com.example.validation_service.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentMessageConsumer.class);

    @Value("${app.kafka.topic.instant-payment-inbound}")
    private String topicName;

    // Placeholder for the validation service
    // @Autowired
    // private ValidationService validationService;

    @KafkaListener(topics = "${app.kafka.topic.instant-payment-inbound}",
                   groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String message) {
        logger.info("Received message from topic {}: {}", topicName, message);
        try {
            // Here, you would typically pass the message to a validation service
            // For now, we'll just log it.
            // validationService.validateAndProcess(message);
            logger.info("Message processed (simulated).");
        } catch (Exception e) {
            logger.error("Error processing message from topic {}: {}", topicName, message, e);
            // Further error handling logic can be added here, e.g., sending to a dead-letter topic
        }
    }
}
