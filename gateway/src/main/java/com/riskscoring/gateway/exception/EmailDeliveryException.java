package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class EmailDeliveryException extends ApiException {

    public EmailDeliveryException(Throwable cause) {
        super(cause, HttpStatus.BAD_GATEWAY, "EMAIL_DELIVERY_FAILED", "error.emailDeliveryFailed");
    }
}
