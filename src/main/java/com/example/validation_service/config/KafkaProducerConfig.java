package com.example.validation_service.config;

import com.example.validation_service.dto.ValidatedPayment;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.producer.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, ValidatedPayment> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Ensure robust delivery

        // Configure JsonSerializer to add type information for deserialization on consumer side
        // This is important if the consumer is not a Spring application or needs explicit type info
        JsonSerializer<ValidatedPayment> jsonSerializer = new JsonSerializer<>();
        jsonSerializer.setAddTypeInfo(false); // Set to true if consumer needs __TypeId__ headers

        return new DefaultKafkaProducerFactory<>(configProps, new StringSerializer(), jsonSerializer);
    }

    @Bean
    public KafkaTemplate<String, ValidatedPayment> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
