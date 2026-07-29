package com.riskscoring.gateway.exception;

public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(Throwable cause) {
        super("Failed to deliver email", cause);
    }
}