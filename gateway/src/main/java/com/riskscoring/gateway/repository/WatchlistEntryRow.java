package com.riskscoring.gateway.repository;

import com.riskscoring.common.model.RiskLevel;

import java.time.Instant;
import java.util.UUID;

public record WatchlistEntryRow(
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