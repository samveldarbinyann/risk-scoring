package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {

    public ForbiddenException() {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", "error.forbidden");
    }
}
