package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class SubscriptionNotFoundException extends ApiException {

    public SubscriptionNotFoundException() {
        super(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", "error.subscriptionNotFound");
    }
}
