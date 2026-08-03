package com.riskscoring.common.model;

import java.time.Instant;
import java.util.List;

public record AddressEvidence(
        String target,
        Chain chain,
        Instant observedAt,
        int ageDays,
        long txCount,
        long txCount24h,
        boolean sampleTruncated,
        String balanceNative,
        List<TokenBalance> tokenBalances,
        int counterpartyCount,
        List<FlaggedExposure> flagged,
        MixerExposure mixerExposure,
        Heuristics heuristics
) implements EvidenceBundle {
}
