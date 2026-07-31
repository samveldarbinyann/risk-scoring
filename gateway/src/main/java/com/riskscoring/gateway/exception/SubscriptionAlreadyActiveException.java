package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class SubscriptionAlreadyActiveException extends ApiException {

    public SubscriptionAlreadyActiveException() {
        super(HttpStatus.CONFLICT, "SUBSCRIPTION_ALREADY_ACTIVE", "error.subscriptionAlreadyActive");
    }
}
