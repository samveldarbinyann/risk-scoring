package com.riskscoring.common.model;

import java.time.Instant;
import java.util.List;

public record EvidenceBundle(
        String address,
        int chainId,
        int ageDays,
        long txCount,
        long txCount24h,
        boolean sampleTruncated,
        Instant observedAt,
        String balanceWei,
        List<TokenBalance> tokenBalances,
        int counterpartyCount,
        List<FlaggedExposure> flagged,
        MixerExposure mixerExposure,
        Heuristics heuristics
) {
}