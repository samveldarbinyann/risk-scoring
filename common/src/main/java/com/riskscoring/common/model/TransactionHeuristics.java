package com.riskscoring.common.model;

public record TransactionHeuristics(
        boolean failed,
        boolean zeroValue,
        boolean roundValue,
        boolean selfTransfer,
        boolean tokenOnly,
        boolean fanOutInternal,
        int distinctPartyCount
) {
}
