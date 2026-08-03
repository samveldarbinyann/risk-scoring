package com.riskscoring.common.model;

import java.time.Instant;
import java.util.List;

public record TransactionEvidence(
        String target,
        Chain chain,
        Instant observedAt,
        String fromAddress,
        String toAddress,
        String valueNative,
        boolean success,
        Instant blockTimestamp,
        int nestedTransferCount,
        int tokenTransferCount,
        List<TransactionParty> parties,
        List<FlaggedExposure> flagged,
        MixerExposure mixerExposure,
        TransactionHeuristics heuristics
) implements EvidenceBundle {
}
