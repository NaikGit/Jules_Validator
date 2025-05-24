# Instant Payment Validation Service

## Overview

This service is a critical component of the Instant Payment Processor. It ingests ISO 20022 XML messages from a Kafka topic, performs structural and business validations, transforms validated messages into an internal JSON format, and publishes them to another Kafka topic. Validation failures and audit events are logged asynchronously to MongoDB.

The service aims for high performance (~5000 TPS) and low latency.

## Prerequisites

*   Java 17 or higher
*   Apache Maven 3.6+
*   Running Kafka instance
*   Running MongoDB instance

## Configuration

All configurations are located in `src/main/resources/application.properties`.

### Core Application
*   `server.port`: Port for the Spring Boot application (if any web endpoints were exposed, default 8080).

### Kafka
*   `spring.kafka.bootstrap-servers`: Kafka broker addresses (e.g., `localhost:9092`).
*   `spring.kafka.consumer.group-id`: Consumer group ID for this service.
*   `app.kafka.topic.instant-payment-inbound`: Input Kafka topic for ISO 20022 messages (default: `instant.payment.inbound`).
*   `app.kafka.topic.instant-payment-validated`: Output Kafka topic for validated JSON messages (default: `instant.payment.validated`).

### MongoDB
*   `spring.data.mongodb.uri`: MongoDB connection URI (e.g., `mongodb://localhost:27017/payment_validation`).
*   `audit.payload.max-length`: Maximum length of the raw payload stored in audit logs (default: `1000`).

### Business Validation Rules
*   `validation.rules.amount.min`: Minimum allowed payment amount.
*   `validation.rules.amount.max`: Maximum allowed payment amount.
*   `validation.rules.currency.allowed`: Comma-separated list of allowed currency codes (e.g., `USD,EUR,GBP`).
*   `validation.rules.participant.supported-prefixes`: Comma-separated list of allowed participant ID prefixes. (Note: actual property key in code is `validation.rules.participant.supported-prefixes` not `allowed-prefixes`)
*   `validation.rules.cutoff.time`: Cutoff time in HH:mm:ss format (e.g., `17:00:00`). Timezone is assumed to be server's local timezone.
*   `validation.rules.duplicate.cache.size`: Maximum size of the in-memory cache for detecting duplicate messages.
*   `validation.rules.duplicate.ttl-seconds`: Time-to-live in seconds for messages in the duplicate detection cache.

## Building the Project

To build the project, run the following Maven command:
```bash
mvn clean install
```

## Running the Service

Once built, you can run the service using:
```bash
java -jar target/validation-service-*.jar
```
(Replace `*` with the actual version of the JAR file generated).

## Input / Output

### Input: Kafka Topic `instant.payment.inbound`
*   **Format:** ISO 20022 PACS.008 XML string.
*   **Example Snippet (Conceptual):**
    ```xml
    <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08">
        <FIToFICstmrCdtTrf>
            <GrpHdr>
                <MsgId>MSGID001</MsgId>
                <CreDtTm>2023-10-26T10:00:00</CreDtTm>
                ...
            </GrpHdr>
            <CdtTrfTxInf>
                <PmtId>
                    <InstrId>INSTRID001</InstrId>
                    <EndToEndId>ENDTOENDID001</EndToEndId>
                </PmtId>
                <InstdAmt Ccy="USD">100.00</InstdAmt>
                <Dbtr>
                    <Nm>Debtor Name</Nm>
                    <Id><OrgId><AnyBIC>DEBTORBIC</AnyBIC></OrgId></Id>
                </Dbtr>
                <Cdtr>
                    <Nm>Creditor Name</Nm>
                    <Id><OrgId><AnyBIC>CREDITORBIC</AnyBIC></OrgId></Id>
                </Cdtr>
                ...
            </CdtTrfTxInf>
        </FIToFICstmrCdtTrf>
    </Document>
    ```

### Output: Kafka Topic `instant.payment.validated`
*   **Format:** JSON
*   **Example:**
    ```json
    {
      "transactionId": "MSGID001",
      "instructionId": "INSTRID001",
      "payer": {
        "name": "Debtor Name",
        "id": "DEBTORBIC"
      },
      "payee": {
        "name": "Creditor Name",
        "id": "CREDITORBIC"
      },
      "amount": 100.00,
      "currency": "USD",
      "timestamp": "2023-10-27T14:35:12.123456Z"
    }
    ```
    *(Note: Debtor/Creditor ID extraction logic might vary based on actual ISO 20022 structure used; example shows BIC)*
    *(Note: Timestamp will be the processing timestamp)*

### Failure Logging: MongoDB Collection `validation_failures`
*   **Database:** Name specified in `spring.data.mongodb.uri` (e.g., `payment_validation`)
*   **Collection:** `validation_failures`
*   **Document Structure:**
    ```json
    {
      "_id": "ObjectId(...)",
      "messageId": "MSGID001",
      "errorType": "business",
      "errorMessage": "Amount exceeds maximum limit.",
      "timestamp": "2023-10-27T14:30:00.000Z",
      "rawPayload": "<truncated XML payload...>"
    }
    ```

## Technology Stack

*   Java 17
*   Spring Boot 3
*   Apache Kafka (Clients)
*   MongoDB (Data, via Spring Data MongoDB)
*   StAX (XML Parsing)
*   MapStruct (DTO Mapping)
*   Micrometer (Metrics - further configuration needed for export)
*   Apache Maven (Build Tool)
