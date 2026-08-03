package com.riskscoring.common.event;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.Verdict;

import java.time.Instant;
import java.util.UUID;

public record ScanCompleted(
        UUID scanId,
        ScanTarget targetType,
        String target,
        Chain chain,
        Verdict verdict,
        String model,
        Instant completedAt
) {
}
