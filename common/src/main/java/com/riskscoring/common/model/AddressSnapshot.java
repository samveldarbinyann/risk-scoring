package com.riskscoring.common.model;

import java.time.Instant;
import java.util.List;

public record AddressSnapshot(
        long txCount,
        long txCount24h,
        String balanceNative,
        List<TokenBalance> tokenBalances,
        Instant firstSeenAt,
        Instant lastSeenAt,
        boolean sampleTruncated,
        Instant observedAt
) {
}
