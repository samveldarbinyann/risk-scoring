package com.riskscoring.common.model;

import java.util.List;

public record EvidenceBundle(
        String address,
        int chainId,
        int ageDays,
        long txCount,
        String balanceWei,
        int counterpartyCount,
        List<FlaggedExposure> flagged,
        MixerExposure mixerExposure,
        Heuristics heuristics
) {
}
