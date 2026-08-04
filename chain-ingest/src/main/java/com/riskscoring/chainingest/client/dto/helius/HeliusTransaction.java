package com.riskscoring.chainingest.client.dto.helius;

import java.util.List;

public record HeliusTransaction(
        String signature,
        long timestamp,
        String feePayer,
        HeliusTransactionError transactionError,
        List<HeliusNativeTransfer> nativeTransfers,
        List<HeliusTokenTransfer> tokenTransfers
) {
}
