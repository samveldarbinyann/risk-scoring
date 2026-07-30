package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ScanGroupReportNotReadyException extends ApiException {

    public ScanGroupReportNotReadyException(UUID groupId) {
        super(HttpStatus.CONFLICT, "SCAN_GROUP_REPORT_NOT_READY", "error.scanGroupReportNotReady", groupId);
    }
}
