package com.example.validation_service.mapper;

import com.example.validation_service.dto.Payee;
import com.example.validation_service.dto.Payer;
import com.example.validation_service.dto.RawPaymentData;
import com.example.validation_service.dto.ValidatedPayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;

@Mapper(componentModel = "spring", imports = {Instant.class, BigDecimal.class, Payer.class, Payee.class})
public abstract class PaymentMapper {

    @Mappings({
        @Mapping(source = "msgId", target = "transactionId"),
        @Mapping(source = "instrId", target = "instructionId"),
        @Mapping(source = "rawData", target = "payer", qualifiedByName = "mapPayer"),
        @Mapping(source = "rawData", target = "payee", qualifiedByName = "mapPayee"),
        @Mapping(source = "amount", target = "amount", qualifiedByName = "stringToBigDecimal"),
        @Mapping(source = "currency", target = "currency"),
        @Mapping(target = "timestamp", expression = "java(Instant.now())")
    })
    public abstract ValidatedPayment toValidatedPayment(RawPaymentData rawData);

    @Named("mapPayer")
    protected Payer mapPayer(RawPaymentData rawData) {
        if (rawData == null) {
            return null;
        }
        Payer payer = new Payer();
        payer.setName(rawData.getDebtorName());
        payer.setId(rawData.getDebtorId());
        return payer;
    }

    @Named("mapPayee")
    protected Payee mapPayee(RawPaymentData rawData) {
        if (rawData == null) {
            return null;
        }
        Payee payee = new Payee();
        payee.setName(rawData.getCreditorName());
        payee.setId(rawData.getCreditorId());
        return payee;
    }

    @Named("stringToBigDecimal")
    protected BigDecimal stringToBigDecimal(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException e) {
            // Consider logging this, though previous validations might catch it.
            // For now, rethrow or return null/default based on error handling strategy.
            // Throwing an exception here might be better to signal bad data.
            throw new IllegalArgumentException("Invalid amount format: " + amount, e);
        }
    }
}
