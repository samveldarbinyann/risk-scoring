package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class NoActiveSubscriptionException extends ApiException {

    public NoActiveSubscriptionException() {
        super(HttpStatus.FORBIDDEN, "NO_ACTIVE_SUBSCRIPTION", "error.noActiveSubscription");
    }
}
