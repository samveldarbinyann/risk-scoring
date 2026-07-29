package com.riskscoring.gateway.exception;

public class TooManyVerificationAttemptsException extends RuntimeException {

    public TooManyVerificationAttemptsException() {
        super("Too many verification attempts for the current code");
    }
}