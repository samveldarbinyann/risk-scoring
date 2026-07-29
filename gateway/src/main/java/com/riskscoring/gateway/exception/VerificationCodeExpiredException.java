package com.riskscoring.gateway.exception;

public class VerificationCodeExpiredException extends RuntimeException {

    public VerificationCodeExpiredException() {
        super("Verification code has expired");
    }
}