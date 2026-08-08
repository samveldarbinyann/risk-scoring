package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class PaymentNotConfiguredException extends ApiException {

    public PaymentNotConfiguredException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_NOT_CONFIGURED", "error.paymentNotConfigured");
    }
}
