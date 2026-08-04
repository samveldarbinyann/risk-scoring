package com.riskscoring.chainingest.client.dto.helius;

import java.util.List;

public record HeliusTransaction(
        String signature,
        long timestamp,
        long slot,
        long fee,
        String feePayer,
        String type,
        String source,
        HeliusTransactionError transactionError,
        List<HeliusNativeTransfer> nativeTransfers,
        List<HeliusTokenTransfer> tokenTransfers
) {
}