package com.example.validation_service.model.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Objects;

@Document(collection = "validation_failures")
public class ValidationFailureLog {

    @Id
    private String id; // MongoDB will generate this

    @Field("message_id")
    private String messageId;

    @Field("error_type")
    private String errorType;

    @Field("error_message")
    private String errorMessage;

    @Field("timestamp")
    private Instant timestamp;

    @Field("raw_payload")
    private String rawPayload;

    // Constructors
    public ValidationFailureLog() {
        this.timestamp = Instant.now();
    }

    public ValidationFailureLog(String messageId, String errorType, String errorMessage, String rawPayload) {
        this.messageId = messageId;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.rawPayload = rawPayload;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationFailureLog that = (ValidationFailureLog) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(messageId, that.messageId) &&
               Objects.equals(errorType, that.errorType) &&
               Objects.equals(errorMessage, that.errorMessage) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(rawPayload, that.rawPayload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, messageId, errorType, errorMessage, timestamp, rawPayload);
    }

    @Override
    public String toString() {
        return "ValidationFailureLog{" +
               "id='" + id + '\'' +
               ", messageId='" + messageId + '\'' +
               ", errorType='" + errorType + '\'' +
               ", errorMessage='" + errorMessage + '\'' +
               ", timestamp=" + timestamp +
               ", rawPayload.length=" + (rawPayload != null ? rawPayload.length() : 0) +
               '}';
    }
}
