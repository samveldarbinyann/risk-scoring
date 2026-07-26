package com.riskscoring.gateway.dto;

import com.riskscoring.common.event.ScanStage;

import java.time.Instant;
import java.util.UUID;

public record ScanProgressMessage(
        UUID scanId,
        ScanStage stage,
        String message,
        Instant at
) {
}