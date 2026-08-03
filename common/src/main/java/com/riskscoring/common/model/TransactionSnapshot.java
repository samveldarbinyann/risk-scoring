package com.riskscoring.common.model;

import java.time.Instant;
import java.util.List;

public record TransactionSnapshot(
        String hash,
        String fromAddress,
        String toAddress,
        String valueNative,
        boolean success,
        Instant blockTimestamp,
        List<TransactionParty> parties,
        int nestedTransferCount,
        int tokenTransferCount,
        Instant observedAt
) {
}
