package com.riskscoring.common.model;

import java.time.Instant;
import java.util.List;

public record AddressEvidence(
        String target,
        int chainId,
        Instant observedAt,
        int ageDays,
        long txCount,
        long txCount24h,
        boolean sampleTruncated,
        String balanceWei,
        List<TokenBalance> tokenBalances,
        int counterpartyCount,
        List<FlaggedExposure> flagged,
        MixerExposure mixerExposure,
        Heuristics heuristics
) implements EvidenceBundle {
}
