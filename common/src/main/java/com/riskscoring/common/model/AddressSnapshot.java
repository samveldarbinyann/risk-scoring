package com.riskscoring.common.model;

import java.time.Instant;

public record AddressSnapshot(
        int ageDays,
        long txCount,
        String balanceWei,
        Instant firstSeenAt,
        Instant lastSeenAt
) {
}