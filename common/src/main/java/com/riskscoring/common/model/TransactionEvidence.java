package com.riskscoring.common.model;

import java.time.Instant;
import java.util.List;

public record TransactionEvidence(
        String target,
        int chainId,
        Instant observedAt,
        String fromAddress,
        String toAddress,
        String valueWei,
        boolean success,
        Instant blockTimestamp,
        int internalTransferCount,
        int erc20TransferCount,
        List<TransactionParty> parties,
        List<FlaggedExposure> flagged,
        MixerExposure mixerExposure,
        TransactionHeuristics heuristics
) implements EvidenceBundle {
}
