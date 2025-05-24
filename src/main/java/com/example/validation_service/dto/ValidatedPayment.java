package com.example.validation_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class ValidatedPayment {

    private String transactionId;
    private String instructionId;
    private Payer payer;
    private Payee payee;
    private BigDecimal amount;
    private String currency;
    private Instant timestamp; // Using Instant for ISO 8601 timestamp

    // Constructors
    public ValidatedPayment() {
    }

    public ValidatedPayment(String transactionId, String instructionId, Payer payer, Payee payee, BigDecimal amount, String currency, Instant timestamp) {
        this.transactionId = transactionId;
        this.instructionId = instructionId;
        this.payer = payer;
        this.payee = payee;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getInstructionId() {
        return instructionId;
    }

    public void setInstructionId(String instructionId) {
        this.instructionId = instructionId;
    }

    public Payer getPayer() {
        return payer;
    }

    public void setPayer(Payer payer) {
        this.payer = payer;
    }

    public Payee getPayee() {
        return payee;
    }

    public void setPayee(Payee payee) {
        this.payee = payee;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidatedPayment that = (ValidatedPayment) o;
        return Objects.equals(transactionId, that.transactionId) &&
               Objects.equals(instructionId, that.instructionId) &&
               Objects.equals(payer, that.payer) &&
               Objects.equals(payee, that.payee) &&
               // Use compareTo for BigDecimal equality
               (amount == null ? that.amount == null : amount.compareTo(that.amount) == 0) &&
               Objects.equals(currency, that.currency) &&
               Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, instructionId, payer, payee, amount, currency, timestamp);
    }

    @Override
    public String toString() {
        return "ValidatedPayment{" +
               "transactionId='" + transactionId + '\'' +
               ", instructionId='" + instructionId + '\'' +
               ", payer=" + payer +
               ", payee=" + payee +
               ", amount=" + amount +
               ", currency='" + currency + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}
