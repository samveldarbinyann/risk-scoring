package com.riskscoring.common.event;

import com.riskscoring.common.model.Language;

import java.time.Instant;
import java.util.UUID;

public record WatchlistAddRequested(
        UUID requestId,
        UUID userId,
        String address,
        int chainId,
        Language language,
        Instant requestedAt
) {
}
