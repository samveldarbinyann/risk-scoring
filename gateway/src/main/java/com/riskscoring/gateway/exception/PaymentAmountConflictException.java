package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class PaymentAmountConflictException extends ApiException {

    public PaymentAmountConflictException(Throwable cause) {
        super(cause, HttpStatus.CONFLICT, "PAYMENT_AMOUNT_CONFLICT", "error.paymentAmountConflict");
    }
}
