package com.riskscoring.gateway.exception;

import lombok.Getter;

@Getter
public class ResendCooldownException extends RuntimeException {

    private final long retryAfterSeconds;

    public ResendCooldownException(long retryAfterSeconds) {
        super("Verification code was requested recently, retry after %s seconds".formatted(retryAfterSeconds));
        this.retryAfterSeconds = retryAfterSeconds;
    }
}