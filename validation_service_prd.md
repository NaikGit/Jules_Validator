# Instant Payment Validation Service – Product Requirements Document (PRD)

## 📌 Overview

- **Product Name:** Instant Payment Validation Service  
- **Owner:** Payments Core Team  
- **Environment:** Java 17+, Spring Boot 3, Kafka, MongoDB  
- **Goal:** Validate and transform ISO 20022 messages into internal payloads with high performance (~5000 TPS) and low latency.

---

## 1. Purpose

This service is a critical component of the Instant Payment Processor. It will:

1. Ingest ISO 20022 XML messages from Kafka.
2. Perform structural and business validations.
3. Transform validated messages into internal JSON format.
4. Publish to an internal Kafka topic.
5. Asynchronously log audit events and validation failures to MongoDB.

---

## 2. Inputs & Outputs

### Input

- **Source:** Kafka Topic (e.g., `instant.payment.inbound`)
- **Payload:** ISO 20022 PACS.008 (XML string)

### Output

- **Target:** Kafka Topic (e.g., `instant.payment.validated`)
- **Payload:** Internal JSON object (see schema below)

---

## 3. Functional Requirements

### 3.1 ISO 20022 Parsing

- Use **StAX (Streaming API for XML)** for low-latency XML parsing.
- Extract required fields:
  - `MsgId`
  - `InstrId`
  - `EndToEndId`
  - `Dbtr.Nm`, `Dbtr.Id`
  - `Cdtr.Nm`, `Cdtr.Id`
  - `Amt`
  - `Currency`
- Fail-fast strategy for malformed XML.

### 3.2 Schema Validation

- Lightweight schema/structure validation.
- Optional XSD check (if it doesn’t affect latency).
- Reject and log messages with invalid structure or missing mandatory tags.

### 3.3 Business Rule Validation

Perform configurable validation checks such as:

- Amount must be within configured min-max range.
- Currency must be from allowed list (e.g., USD, EUR).
- Debtor and Creditor must belong to supported participants.
- Validate against cutoff time (configurable, e.g., 5 PM EST).
- Detect and reject duplicate messages (via Redis or in-memory LRU cache).

### 3.4 Transformation

Transform the validated ISO 20022 message into the internal JSON schema:

```json
{
  "transactionId": "<MsgId>",
  "instructionId": "<InstrId>",
  "payer": {
    "name": "<Dbtr.Nm>",
    "id": "<Dbtr.Id>"
  },
  "payee": {
    "name": "<Cdtr.Nm>",
    "id": "<Cdtr.Id>"
  },
  "amount": 1000.25,
  "currency": "USD",
  "timestamp": "<ISO 8601 timestamp>"
}
```

### 3.5 Kafka Output

- Valid messages are written to the `instant.payment.validated` Kafka topic.
- Use either **JSON** or **Protobuf** for serialization (based on downstream need).

### 3.6 Error Handling & Logging

- Failures are written to MongoDB (`validation_failures` collection).
- Include fields:
  - `messageId`
  - `errorType` (`schema`, `business`, `parsing`)
  - `timestamp`
  - `rawPayload` (hashed or truncated if large)
- All logs should be **asynchronous** to avoid blocking validation flow.

---

## 4. Non-Functional Requirements

| Property         | Requirement                             |
|------------------|------------------------------------------|
| **Throughput**   | ≥ 5000 TPS sustained                     |
| **Latency**      | ≤ 10ms per message (p99)                 |
| **Availability** | 99.99% uptime                            |
| **Scalability**  | Horizontally scalable via Kafka partitioning |
| **Security**     | Input size limits, protect against XML bombs (XXE) |
| **Observability**| Metrics for latency, throughput, error types |
| **Configurability** | Rule thresholds and cutoffs via YAML or DB |
| **Idempotency (optional)** | Redis-based or LRU cache-based deduplication |

---

## 5. Technology Stack

| Component         | Technology             |
|------------------|------------------------|
| Language          | Java 17+               |
| Framework         | Spring Boot 3          |
| Messaging         | Apache Kafka           |
| XML Parser        | StAX                   |
| DTO Mapper        | Manual or MapStruct    |
| Async DB Logging  | MongoDB                |
| Metrics           | Micrometer + Prometheus|
| Config Management | Spring Config Server or DB Config Table |

---

## 6. Testing & Validation Strategy

### Unit Tests

- For all validation rules (amount, currency, participant)
- ISO XML parsing and field extraction

### Integration Tests

- End-to-end test with real ISO 20022 payloads

### Performance Tests

- Simulate ≥ 5000 TPS using Gatling or Locust
- Validate latency, memory, and GC performance

### Negative Test Cases

- Malformed XML
- Unsupported currency
- Amount out of range
- Duplicate transaction ID
- Cutoff time breach

---

## 7. Optional Enhancements

- **Rule Toggle Support:** Enable/disable specific validation rules at runtime
- **Audit Trail Service:** Separate logging service using Kafka + MongoDB
- **Dead Letter Queue:** Kafka topic for hard-failure messages
- **Alerting:** Slack or Email alerts on failure spikes or threshold breaches

---

## ✅ Ready for Code Generation

This PRD is structured to support code generation by tools like Agentic IDE.  
Modules to scaffold:

- Kafka consumer/producer configuration
- StAX-based XML field extractor
- Validation engine with pluggable rules
- Internal payload DTO and mapper
- MongoDB logging and metrics integration