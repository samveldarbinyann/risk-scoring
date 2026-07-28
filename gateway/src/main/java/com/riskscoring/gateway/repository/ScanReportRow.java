package com.riskscoring.gateway.repository;

import com.riskscoring.common.model.RiskLevel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ScanReportRow(
        UUID scanId,
        String address,
        int chainId,
        RiskLevel riskLevel,
        int score,
        String explanation,
        List<String> decisiveSignals,
        List<String> manualChecks,
        String model,
        Instant createdAt
) {
}
