package com.riskscoring.common.event;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;

import java.time.Instant;
import java.util.UUID;

public record SignalsComputed(
        UUID scanId,
        ScanTarget targetType,
        String target,
        Chain chain,
        EvidenceBundle evidence,
        Language language,
        Instant computedAt
) {
}
