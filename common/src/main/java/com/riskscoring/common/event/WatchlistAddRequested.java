package com.riskscoring.common.event;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;

import java.time.Instant;
import java.util.UUID;

public record WatchlistAddRequested(
        UUID requestId,
        UUID userId,
        String address,
        Chain chain,
        Language language,
        Instant requestedAt
) {
}
