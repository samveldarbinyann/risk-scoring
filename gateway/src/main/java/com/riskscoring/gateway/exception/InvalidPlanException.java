package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class InvalidPlanException extends ApiException {

    public InvalidPlanException(String planCode) {
        super(HttpStatus.BAD_REQUEST, "INVALID_PLAN", "error.invalidPlan", planCode);
    }
}
