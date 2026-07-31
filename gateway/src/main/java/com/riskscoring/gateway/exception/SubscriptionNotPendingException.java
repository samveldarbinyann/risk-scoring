package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class SubscriptionNotPendingException extends ApiException {

    public SubscriptionNotPendingException(UUID id) {
        super(HttpStatus.CONFLICT, "SUBSCRIPTION_NOT_PENDING", "error.subscriptionNotPending", id);
    }
}
