package com.riskscoring.gateway.repository;

import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.common.model.TokenBalance;

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
        String balanceWei,
        long txCount,
        long txCount24h,
        boolean sampleTruncated,
        Instant observedAt,
        List<TokenBalance> tokenBalances,
        String model,
        Instant createdAt
) {
}
