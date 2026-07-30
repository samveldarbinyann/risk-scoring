package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class InvalidVerificationCodeException extends ApiException {

    public InvalidVerificationCodeException() {
        super(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_CODE", "error.invalidVerificationCode");
    }
}
