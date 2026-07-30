package com.riskscoring.common.event;

import com.riskscoring.common.model.RiskLevel;

import java.time.Instant;
import java.util.UUID;

public record AlertTriggered(
        UUID alertId,
        UUID watchlistEntryId,
        UUID userId,
        String address,
        int chainId,
        RiskLevel previousRiskLevel,
        int previousScore,
        RiskLevel newRiskLevel,
        int newScore,
        UUID scanId,
        Instant triggeredAt
) {
}
