package com.example.validation_service.consumer;

import com.example.validation_service.AbstractKafkaIntegrationTest;
import com.example.validation_service.dto.ValidatedPayment;
import com.example.validation_service.service.AuditService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;


@SpringBootTest
@DirtiesContext // Ensures Spring context is reset between test classes, helpful with Testcontainers
public class PaymentMessageConsumerIT extends AbstractKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaProducerTemplate; // For producing to input topic

    @MockBean
    private AuditService auditService;

    private KafkaMessageListenerContainer<String, String> outputTopicListenerContainer;
    private BlockingQueue<ConsumerRecord<String, String>> outputTopicRecords;
    
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());


    // Override MongoDB URI to a dummy one since AuditService is mocked
    @DynamicPropertySource
    static void overrideMongoUri(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://dummyuser:dummypass@localhost:27017/dummytestdb");
    }


    @BeforeEach
    void setUpOutputTopicListener() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", kafkaContainer.getBootstrapServers());
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        ContainerProperties containerProps = new ContainerProperties(OUTPUT_TOPIC);
        outputTopicListenerContainer = new KafkaMessageListenerContainer<>(cf, containerProps);
        outputTopicRecords = new LinkedBlockingQueue<>();
        outputTopicListenerContainer.setupMessageListener((MessageListener<String, String>) outputTopicRecords::add);
        outputTopicListenerContainer.start();
    }
    
    private String createValidXml(String msgId, String instrId, String endToEndId, String debtorName, String debtorId, String creditorName, String creditorId, String amount, String currency) {
        return String.format(
            "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08\">" +
            "  <FIToFICstmrCdtTrf>" +
            "    <GrpHdr>" +
            "      <MsgId>%s</MsgId>" +
            "      <CreDtTm>2023-10-26T10:00:00</CreDtTm>" +
            "    </GrpHdr>" +
            "    <CdtTrfTxInf>" +
            "      <PmtId>" +
            "        <InstrId>%s</InstrId>" +
            "        <EndToEndId>%s</EndToEndId>" +
            "      </PmtId>" +
            "      <InstdAmt Ccy=\"%s\">%s</InstdAmt>" +
            "      <Dbtr>" +
            "        <Nm>%s</Nm>" +
            "        <Id><OrgId><AnyBIC>%s</AnyBIC></OrgId></Id>" +
            "      </Dbtr>" +
            "      <Cdtr>" +
            "        <Nm>%s</Nm>" +
            "        <Id><OrgId><AnyBIC>%s</AnyBIC></OrgId></Id>" +
            "      </Cdtr>" +
            "    </CdtTrfTxInf>" +
            "  </FIToFICstmrCdtTrf>" +
            "</Document>",
            msgId, instrId, endToEndId, currency, amount, debtorName, debtorId, creditorName, creditorId
        );
    }


    @Test
    void testProcessPayment_ValidMessage_TransformsAndPublishes() throws Exception {
        String msgId = "VALIDMSG001";
        String instrId = "VALIDINSTR001";
        String validXml = createValidXml(msgId, instrId, "E2E001", "Debtor Valid", "DBTRVALID", "Creditor Valid", "CDTRVALID", "100.50", "EUR");

        kafkaProducerTemplate.send(INPUT_TOPIC, msgId, validXml).get(10, TimeUnit.SECONDS); // Ensure send completes

        ConsumerRecord<String, String> received = outputTopicRecords.poll(10, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.key()).isEqualTo(msgId);

        ValidatedPayment validatedPayment = objectMapper.readValue(received.value(), ValidatedPayment.class);
        assertThat(validatedPayment.getTransactionId()).isEqualTo(msgId);
        assertThat(validatedPayment.getInstructionId()).isEqualTo(instrId);
        assertThat(validatedPayment.getAmount()).isEqualTo(new BigDecimal("100.50"));
        assertThat(validatedPayment.getCurrency()).isEqualTo("EUR");
        assertThat(validatedPayment.getPayer().getName()).isEqualTo("Debtor Valid");
        assertThat(validatedPayment.getPayee().getName()).isEqualTo("Creditor Valid");
        assertThat(validatedPayment.getTimestamp()).isNotNull();

        verify(auditService, never()).logFailure(anyString(), anyString(), anyString(), anyString());
        
        // Stop the listener container
        if (outputTopicListenerContainer != null) {
            outputTopicListenerContainer.stop();
        }
    }

    @Test
    void testProcessPayment_InvalidXml_NoOutputAndFailureLogged() throws Exception {
        String malformedXml = "<Document><UnclosedTag></Document>";
        String msgId = "MALFORMED001"; // Attempt to give it some context if possible, though parsing might fail before extracting.

        kafkaProducerTemplate.send(INPUT_TOPIC, msgId, malformedXml).get(10, TimeUnit.SECONDS);

        // Verify no message on output topic
        ConsumerRecord<String, String> received = outputTopicRecords.poll(5, TimeUnit.SECONDS); // Shorter timeout
        assertThat(received).isNull();

        // Verify AuditService.logFailure was called
        // The exact MsgId might be null or the one we sent, depending on when parsing fails.
        // For malformed XML, the msgId might not be extractable.
        verify(auditService, timeout(5000).times(1))
            .logFailure(eq(null), // Or eq(msgId) if it can be determined before full parsing
                        eq("parsing"), 
                        anyString(), 
                        eq(malformedXml));
                        
        if (outputTopicListenerContainer != null) {
            outputTopicListenerContainer.stop();
        }
    }
    
    @Test
    void testProcessPayment_BusinessRuleViolation_AmountTooHigh_NoOutputAndFailureLogged() throws Exception {
        String msgId = "BIZFAIL001";
        // This amount (5,000,000) should exceed the default max amount (1,000,000)
        String xmlWithHighAmount = createValidXml(msgId, "INSTRBIZ001", "E2EBIZ001", "Debtor Biz", "DBTRBIZ", "Creditor Biz", "CDTRBIZ", "5000000.00", "USD");

        kafkaProducerTemplate.send(INPUT_TOPIC, msgId, xmlWithHighAmount).get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> received = outputTopicRecords.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNull();

        verify(auditService, timeout(5000).times(1))
            .logFailure(eq(msgId), 
                        eq("business"), 
                        anyString(), // Error message can be flexible
                        eq(xmlWithHighAmount));
                        
        if (outputTopicListenerContainer != null) {
            outputTopicListenerContainer.stop();
        }
    }
}
