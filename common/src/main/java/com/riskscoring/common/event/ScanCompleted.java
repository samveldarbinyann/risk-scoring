package com.riskscoring.common.event;

import com.riskscoring.common.model.Verdict;

import java.time.Instant;
import java.util.UUID;

public record ScanCompleted(
        UUID scanId,
        String address,
        int chainId,
        Verdict verdict,
        String model,
        Instant completedAt
) {
}
