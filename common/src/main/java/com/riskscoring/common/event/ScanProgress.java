package com.riskscoring.common.event;

import com.riskscoring.common.model.Language;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ScanProgress(
        UUID scanId,
        ScanStage stage,
        String messageKey,
        List<Object> messageArgs,
        Language language,
        Instant at
) {
}
