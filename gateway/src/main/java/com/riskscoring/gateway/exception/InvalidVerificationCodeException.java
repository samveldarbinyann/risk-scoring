package com.riskscoring.gateway.exception;

public class InvalidVerificationCodeException extends RuntimeException {

    public InvalidVerificationCodeException() {
        super("Verification code is invalid or already used");
    }
}