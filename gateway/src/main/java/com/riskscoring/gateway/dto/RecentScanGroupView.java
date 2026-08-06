package com.riskscoring.gateway.dto;

import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.common.model.ScanTarget;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecentScanGroupView(
        UUID groupId,
        ScanTarget targetType,
        String target,
        List<Chain> chains,
        boolean completed,
        RiskLevel worstRiskLevel,
        Integer worstScore,
        Instant requestedAt,
        ScanSource source
) {
}
