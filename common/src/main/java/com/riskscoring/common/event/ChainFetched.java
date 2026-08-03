package com.riskscoring.common.event;

import com.riskscoring.common.model.ChainFacts;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;

import java.time.Instant;
import java.util.UUID;

public record ChainFetched(
        UUID scanId,
        ScanTarget targetType,
        String target,
        int chainId,
        ChainFacts facts,
        Language language,
        Instant fetchedAt
) {
}
