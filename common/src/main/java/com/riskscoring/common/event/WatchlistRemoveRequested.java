package com.riskscoring.common.event;

import java.time.Instant;
import java.util.UUID;

public record WatchlistRemoveRequested(
        UUID requestId,
        UUID userId,
        UUID entryId,
        Instant requestedAt
) {
}
