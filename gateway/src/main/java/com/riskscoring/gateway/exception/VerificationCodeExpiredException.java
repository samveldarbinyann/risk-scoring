package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class VerificationCodeExpiredException extends ApiException {

    public VerificationCodeExpiredException() {
        super(HttpStatus.GONE, "VERIFICATION_CODE_EXPIRED", "error.verificationCodeExpired");
    }
}
