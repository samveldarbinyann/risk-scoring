package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class ResendCooldownException extends ApiException {

    public ResendCooldownException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, "RESEND_COOLDOWN", "error.resendCooldown", retryAfterSeconds);
    }
}
