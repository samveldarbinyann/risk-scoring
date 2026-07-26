package com.riskscoring.riskai.exception;

public class InvalidVerdictException extends RuntimeException {

    public InvalidVerdictException(String message) {
        super(message);
    }

    public InvalidVerdictException(String message, Throwable cause) {
        super(message, cause);
    }
}
