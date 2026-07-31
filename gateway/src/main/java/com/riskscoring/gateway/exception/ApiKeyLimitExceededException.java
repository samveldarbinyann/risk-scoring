package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class ApiKeyLimitExceededException extends ApiException {

    public ApiKeyLimitExceededException(int limit) {
        super(HttpStatus.CONFLICT, "API_KEY_LIMIT_EXCEEDED", "error.apiKeyLimitExceeded", limit);
    }
}
