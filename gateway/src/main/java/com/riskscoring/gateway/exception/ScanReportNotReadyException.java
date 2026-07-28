package com.riskscoring.gateway.exception;

import com.riskscoring.common.event.ScanStage;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ScanReportNotReadyException extends RuntimeException {

    private final UUID scanId;
    private final ScanStage status;

    public ScanReportNotReadyException(UUID scanId, ScanStage status) {
        super("Scan %s is not completed yet, current status: %s".formatted(scanId, status));
        this.scanId = scanId;
        this.status = status;
    }
}