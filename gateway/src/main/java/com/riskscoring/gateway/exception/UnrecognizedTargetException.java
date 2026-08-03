package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class UnrecognizedTargetException extends ApiException {

    public UnrecognizedTargetException(String target) {
        super(HttpStatus.BAD_REQUEST, "UNRECOGNIZED_TARGET", "error.unrecognizedTarget", target);
    }
}
