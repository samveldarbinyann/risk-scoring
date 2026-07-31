package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ApiKeyNotFoundException extends ApiException {

    public ApiKeyNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "API_KEY_NOT_FOUND", "error.apiKeyNotFound", id);
    }
}
