package com.riskscoring.gateway.repository;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.common.model.ScanTarget;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ScanReportRow(
        UUID scanId,
        ScanTarget targetType,
        String target,
        Chain chain,
        RiskLevel riskLevel,
        int score,
        String explanation,
        List<String> decisiveSignals,
        List<String> manualChecks,
        Instant observedAt,
        EvidenceBundle evidence,
        String model,
        Instant createdAt
) {
}
