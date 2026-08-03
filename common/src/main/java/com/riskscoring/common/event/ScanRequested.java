package com.riskscoring.common.event;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;

import java.time.Instant;
import java.util.UUID;

public record ScanRequested(
        UUID scanId,
        ScanTarget targetType,
        String target,
        Chain chain,
        Instant requestedAt,
        ScanSource source,
        Language language
) {
}
