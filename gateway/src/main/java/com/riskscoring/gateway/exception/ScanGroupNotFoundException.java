package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ScanGroupNotFoundException extends ApiException {

    public ScanGroupNotFoundException(UUID groupId) {
        super(HttpStatus.NOT_FOUND, "SCAN_GROUP_NOT_FOUND", "error.scanGroupNotFound", groupId);
    }
}
