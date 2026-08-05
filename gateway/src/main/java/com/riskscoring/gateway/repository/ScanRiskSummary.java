package com.riskscoring.gateway.repository;

import com.riskscoring.common.model.RiskLevel;

import java.util.UUID;

public record ScanRiskSummary(
        UUID scanId,
        RiskLevel riskLevel,
        int score
) {
}
