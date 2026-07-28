package com.riskscoring.common.model;

import java.time.Instant;
import java.util.List;

public record AddressSnapshot(
        int ageDays,
        long txCount,
        long txCount24h,
        String balanceWei,
        List<TokenBalance> tokenBalances,
        Instant firstSeenAt,
        Instant lastSeenAt,
        boolean sampleTruncated
) {
}