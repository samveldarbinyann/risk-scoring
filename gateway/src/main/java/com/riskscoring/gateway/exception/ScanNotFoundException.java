package com.riskscoring.gateway.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ScanNotFoundException extends RuntimeException {

    private final UUID scanId;

    public ScanNotFoundException(UUID scanId) {
        super("Scan not found: " + scanId);
        this.scanId = scanId;
    }
}