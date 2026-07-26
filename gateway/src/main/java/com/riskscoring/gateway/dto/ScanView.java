package com.riskscoring.gateway.dto;

import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.event.ScanStage;

import java.time.Instant;
import java.util.UUID;

public record ScanView(
        UUID scanId,
        String address,
        int chainId,
        ScanStage status,
        ScanSource source,
        Instant requestedAt,
        Instant completedAt
) {
}