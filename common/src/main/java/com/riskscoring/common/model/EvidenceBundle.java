package com.riskscoring.common.model;

import java.util.List;

public record EvidenceBundle(
        String address,
        int chainId,
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
) {
}