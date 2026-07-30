package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ScanNotFoundException extends ApiException {

    public ScanNotFoundException(UUID scanId) {
        super(HttpStatus.NOT_FOUND, "SCAN_NOT_FOUND", "error.scanNotFound", scanId);
    }
}
