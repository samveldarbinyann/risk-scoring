package com.riskscoring.common.event;

import java.time.Instant;
import java.util.UUID;

public record ScanProgress(
        UUID scanId,
        ScanStage stage,
        String message,
        Instant at
) {
}