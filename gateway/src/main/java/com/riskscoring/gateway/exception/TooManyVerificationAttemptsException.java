package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class TooManyVerificationAttemptsException extends ApiException {

    public TooManyVerificationAttemptsException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS", "error.tooManyVerificationAttempts");
    }
}
