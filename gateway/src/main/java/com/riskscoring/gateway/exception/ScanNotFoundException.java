package com.riskscoring.gateway.exception;

import java.util.UUID;

public class ScanNotFoundException extends RuntimeException {

    public ScanNotFoundException(UUID scanId) {
        super("Scan not found: " + scanId);
    }
}