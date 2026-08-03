package com.riskscoring.common.model;

import java.time.Instant;
import java.util.List;

public record TransactionSnapshot(
        String hash,
        String fromAddress,
        String toAddress,
        String valueWei,
        boolean success,
        Instant blockTimestamp,
        List<TransactionParty> parties,
        int internalTransferCount,
        int erc20TransferCount,
        Instant observedAt
) {
}
