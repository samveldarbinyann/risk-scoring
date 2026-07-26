package com.riskscoring.common.event;

import java.time.Instant;
import java.util.UUID;

public record   ScanRequested(
        UUID scanId,
        String address,
        int chainId,
        Instant requestedAt,
        ScanSource source
) {
}