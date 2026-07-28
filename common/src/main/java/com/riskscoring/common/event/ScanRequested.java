package com.riskscoring.common.event;

import com.riskscoring.common.model.Language;

import java.time.Instant;
import java.util.UUID;

public record ScanRequested(
        UUID scanId,
        String address,
        int chainId,
        Instant requestedAt,
        ScanSource source,
        Language language
) {
}
