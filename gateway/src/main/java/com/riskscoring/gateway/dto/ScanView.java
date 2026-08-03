package com.riskscoring.gateway.dto;

import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.model.ScanTarget;

import java.time.Instant;
import java.util.UUID;

public record ScanView(
        UUID scanId,
        ScanTarget targetType,
        String target,
        int chainId,
        ScanStage status,
        ScanSource source,
        Instant requestedAt,
        Instant completedAt
) {
}
