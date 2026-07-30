package com.riskscoring.gateway.dto;

import com.riskscoring.common.model.RiskLevel;

import java.time.Instant;
import java.util.UUID;

public record WatchlistEntryView(
        UUID id,
        String address,
        int chainId,
        RiskLevel lastRiskLevel,
        Integer lastScore,
        UUID lastScanId,
        Instant lastCheckedAt,
        Instant createdAt
) {
}
