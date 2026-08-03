package com.riskscoring.gateway.repository;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.RiskLevel;

import java.time.Instant;
import java.util.UUID;

public record AlertRow(
        UUID id,
        UUID watchlistEntryId,
        String address,
        Chain chain,
        RiskLevel previousRiskLevel,
        int previousScore,
        RiskLevel newRiskLevel,
        int newScore,
        UUID scanId,
        Instant triggeredAt
) {
}
