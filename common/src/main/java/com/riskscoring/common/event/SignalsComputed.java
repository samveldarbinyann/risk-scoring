package com.riskscoring.common.event;

import com.riskscoring.common.model.EvidenceBundle;

import java.time.Instant;
import java.util.UUID;

public record SignalsComputed(
        UUID scanId,
        String address,
        int chainId,
        EvidenceBundle evidence,
        Instant computedAt
) {
}
