package com.example.validation_service.dto;

import java.util.Objects;

public class RawPaymentData {

    private String msgId;
    private String instrId;
    private String endToEndId;
    private String debtorName;
    private String debtorId; // Assuming this will be a string representation of the ID
    private String creditorName;
    private String creditorId; // Assuming this will be a string representation of the ID
    private String amount;
    private String currency;

    // Constructors
    public RawPaymentData() {
    }

    public RawPaymentData(String msgId, String instrId, String endToEndId, String debtorName, String debtorId, String creditorName, String creditorId, String amount, String currency) {
        this.msgId = msgId;
        this.instrId = instrId;
        this.endToEndId = endToEndId;
        this.debtorName = debtorName;
        this.debtorId = debtorId;
        this.creditorName = creditorName;
        this.creditorId = creditorId;
        this.amount = amount;
        this.currency = currency;
    }

    // Getters and Setters
    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getInstrId() {
        return instrId;
    }

    public void setInstrId(String instrId) {
        this.instrId = instrId;
    }

    public String getEndToEndId() {
        return endToEndId;
    }

    public void setEndToEndId(String endToEndId) {
        this.endToEndId = endToEndId;
    }

    public String getDebtorName() {
        return debtorName;
    }

    public void setDebtorName(String debtorName) {
        this.debtorName = debtorName;
    }

    public String getDebtorId() {
        return debtorId;
    }

    public void setDebtorId(String debtorId) {
        this.debtorId = debtorId;
    }

    public String getCreditorName() {
        return creditorName;
    }

    public void setCreditorName(String creditorName) {
        this.creditorName = creditorName;
    }

    public String getCreditorId() {
        return creditorId;
    }

    public void setCreditorId(String creditorId) {
        this.creditorId = creditorId;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RawPaymentData that = (RawPaymentData) o;
        return Objects.equals(msgId, that.msgId) &&
               Objects.equals(instrId, that.instrId) &&
               Objects.equals(endToEndId, that.endToEndId) &&
               Objects.equals(debtorName, that.debtorName) &&
               Objects.equals(debtorId, that.debtorId) &&
               Objects.equals(creditorName, that.creditorName) &&
               Objects.equals(creditorId, that.creditorId) &&
               Objects.equals(amount, that.amount) &&
               Objects.equals(currency, that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(msgId, instrId, endToEndId, debtorName, debtorId, creditorName, creditorId, amount, currency);
    }

    @Override
    public String toString() {
        return "RawPaymentData{" +
               "msgId='" + msgId + '\'' +
               ", instrId='" + instrId + '\'' +
               ", endToEndId='" + endToEndId + '\'' +
               ", debtorName='" + debtorName + '\'' +
               ", debtorId='" + debtorId + '\'' +
               ", creditorName='" + creditorName + '\'' +
               ", creditorId='" + creditorId + '\'' +
               ", amount='" + amount + '\'' +
               ", currency='" + currency + '\'' +
               '}';
    }
}
