package com.example.validation_service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Testcontainers
public abstract class AbstractKafkaIntegrationTest {

    protected static final String KAFKA_IMAGE_NAME = "confluentinc/cp-kafka:7.5.3";

    @Container
    protected static final KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE_NAME));

    protected static final String INPUT_TOPIC = "instant.payment.inbound";
    protected static final String OUTPUT_TOPIC = "instant.payment.validated";

    @BeforeAll
    static void startKafkaContainer() {
        kafkaContainer.start();
        createTopics(INPUT_TOPIC, OUTPUT_TOPIC);
    }
    
    @AfterAll
    static void stopKafkaContainer() {
        kafkaContainer.stop();
    }


    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafkaContainer::getBootstrapServers);
        // For tests, we often want to ensure topics are auto-created or ensure tests create them.
        // Spring Boot's auto-creation might interfere or help depending on the test strategy.
        // For now, we explicitly create topics.
        registry.add("spring.kafka.consumer.properties.auto.offset.reset", () -> "earliest");
    }

    private static void createTopics(String... topicNames) {
        Map<String, Object> config = Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        try (AdminClient admin = AdminClient.create(config)) {
            List<NewTopic> newTopics = Stream.of(topicNames)
                                             .map(topic -> new NewTopic(topic, 1, (short) 1))
                                             .collect(Collectors.toList());
            admin.createTopics(newTopics).all().get();
            System.out.println("Created topics: " + String.join(", ", topicNames));
        } catch (ExecutionException | InterruptedException e) {
            if (e.getCause() instanceof org.apache.kafka.common.errors.TopicExistsException) {
                System.out.println("Topics already exist, proceeding: " + String.join(", ", topicNames));
            } else {
                throw new RuntimeException("Failed to create topics", e);
            }
        }
    }
}
