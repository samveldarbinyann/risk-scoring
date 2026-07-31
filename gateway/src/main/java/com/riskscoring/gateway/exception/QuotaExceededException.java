package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class QuotaExceededException extends ApiException {

    public QuotaExceededException(int limit, int used, int requested) {
        super(HttpStatus.TOO_MANY_REQUESTS, "QUOTA_EXCEEDED", "error.quotaExceeded", limit, used, requested);
    }
}
