package com.riskscoring.gateway.exception;

import com.riskscoring.common.event.ScanStage;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ScanReportNotReadyException extends ApiException {

    public ScanReportNotReadyException(UUID scanId, ScanStage status) {
        super(HttpStatus.CONFLICT, "REPORT_NOT_READY", "error.scanReportNotReady", scanId, status);
    }
}
