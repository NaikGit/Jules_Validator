package com.example.validation_service.service;

import com.example.validation_service.dto.RawPaymentData;
import com.example.validation_service.dto.ValidatedPayment;
import com.example.validation_service.mapper.PaymentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentTransformerService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentTransformerService.class);

    private final PaymentMapper paymentMapper;

    @Autowired
    public PaymentTransformerService(PaymentMapper paymentMapper) {
        this.paymentMapper = paymentMapper;
    }

    public ValidatedPayment transform(RawPaymentData rawData) throws TransformationException {
        if (rawData == null) {
            logger.warn("RawPaymentData is null, cannot transform.");
            throw new TransformationException("Input RawPaymentData cannot be null.");
        }

        try {
            logger.debug("Transforming RawPaymentData with MsgId: {}", rawData.getMsgId());
            ValidatedPayment validatedPayment = paymentMapper.toValidatedPayment(rawData);
            logger.info("Successfully transformed RawPaymentData with MsgId {} to ValidatedPayment.", rawData.getMsgId());
            return validatedPayment;
        } catch (IllegalArgumentException e) {
            // This could be thrown by our custom stringToBigDecimal if format is bad
            logger.error("Transformation failed for MsgId {}: {}", rawData.getMsgId(), e.getMessage(), e);
            throw new TransformationException("Error during data transformation: " + e.getMessage(), e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions from MapStruct or logic
            logger.error("Unexpected error during transformation for MsgId {}: {}", rawData.getMsgId(), e.getMessage(), e);
            throw new TransformationException("An unexpected error occurred during data transformation for MsgId " + rawData.getMsgId(), e);
        }
    }
}
