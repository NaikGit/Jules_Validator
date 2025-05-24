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

## Deployment

This section outlines how to deploy the Instant Payment Validation Service.

### Local Machine Deployment

1.  **Ensure Prerequisites are Running:**
    *   A local Kafka instance must be running.
    *   A local MongoDB instance must be running.
2.  **Configure Application:**
    *   Verify that the `src/main/resources/application.properties` file is configured to connect to your local Kafka and MongoDB instances (e.g., `spring.kafka.bootstrap-servers=localhost:9092`, `spring.data.mongodb.uri=mongodb://localhost:27017/payment_validation`).
3.  **Build the Application:**
    *   If not already done, build the project using Maven:
        ```bash
        mvn clean install
        ```
4.  **Run the Application:**
    *   Execute the JAR file generated in the `target/` directory:
        ```bash
        java -jar target/validation-service-*.jar
        ```
        (Replace `*` with the actual version of the JAR).
    *   The service will start, connect to Kafka and MongoDB, and begin processing messages from the input topic.

### Development Environment Deployment

Deploying to a development environment (or any other shared environment like staging, UAT) typically involves more structured processes and may vary depending on the environment's setup.

1.  **Build the JAR:**
    *   As with local deployment, a JAR file is built using `mvn clean install`.
2.  **Configuration Management:**
    *   **Environment-Specific Profiles:** Spring Boot allows for environment-specific properties files (e.g., `application-dev.properties`, `application-staging.properties`). The active profile can be set via environment variables (`SPRING_PROFILES_ACTIVE=dev`) when running the application. These files would contain the Kafka, MongoDB, and other configurations specific to that environment.
    *   **Externalized Configuration:** In many setups, configurations are externalized using tools like:
        *   Spring Cloud Config Server
        *   Kubernetes ConfigMaps and Secrets
        *   Environment variables directly injected into the application.
    *   Ensure the application is packaged or configured to pick up the correct settings for the target development environment.
3.  **Deployment Methods:**
    *   **Directly on a Server:** The JAR file can be copied to a server and run using `java -jar ...` (often managed by a process manager like `systemd` or `supervisor`).
    *   **Containerization (Docker):**
        *   A `Dockerfile` would be created to package the application JAR into a Docker image.
        *   This image is then pushed to a container registry.
        *   The application is run as a container (e.g., using Docker Compose for single-host setups or Kubernetes for orchestrated environments).
        *   Example `Dockerfile` snippet:
            ```dockerfile
            FROM openjdk:17-jdk-slim
            ARG JAR_FILE=target/*.jar
            COPY ${JAR_FILE} app.jar
            ENTRYPOINT ["java","-jar","/app.jar"]
            ```
    *   **CI/CD Pipelines:** Deployment to development environments is often automated using CI/CD pipelines (e.g., Jenkins, GitLab CI, GitHub Actions) which build, test, and deploy the application.

4.  **Dependencies:**
    *   Ensure the development environment has network access to the required Kafka brokers and MongoDB instances for that environment.

This service is designed as a standard Spring Boot application, making it compatible with common Java deployment practices.

## CI/CD with GitHub Actions

This project is configured with a GitHub Actions workflow to automate the building and deployment of the application to Docker Hub.

### Workflow Overview

*   **Workflow File:** `.github/workflows/deploy.yml`
*   **Trigger:** The workflow automatically runs on every `push` to the `main` branch.
*   **Jobs:**
    1.  **Build and Push to Docker Hub (`build_and_push_to_dockerhub`):**
        *   Checks out the source code.
        *   Sets up JDK 17.
        *   Caches Maven dependencies for faster builds.
        *   Builds the Spring Boot application using Maven (`mvn clean install -DskipTests`).
        *   Sets up Docker Buildx.
        *   Logs into Docker Hub using credentials stored in GitHub Secrets.
        *   Extracts metadata for Docker image tagging (uses commit SHA and `latest`).
        *   Builds the Docker image using the provided `Dockerfile`.
        *   Pushes the built Docker image to Docker Hub. The image will be named `your-dockerhub-username/validation-service` (ensure your Docker Hub username is correctly configured in secrets and the image name in the workflow is as desired).

### Required GitHub Secrets

To enable the workflow to push to Docker Hub, you need to configure the following secrets in your GitHub repository settings (under "Settings" > "Secrets and variables" > "Actions"):

*   `DOCKERHUB_USERNAME`: Your Docker Hub username.
*   `DOCKERHUB_TOKEN`: Your Docker Hub access token. It is strongly recommended to use an access token with appropriate permissions rather than your Docker Hub password.

### Docker Image Naming

The pushed Docker image will be tagged with:
*   The commit SHA (e.g., `your-dockerhub-username/validation-service:abcdef1234567890`)
*   `latest` (for pushes to the `main` branch, e.g., `your-dockerhub-username/validation-service:latest`)

Please replace `your-dockerhub-username/validation-service` in the workflow file (`.github/workflows/deploy.yml` in the `docker/metadata-action` step, under `images`) if you wish to use a different Docker Hub repository name or organization.

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
