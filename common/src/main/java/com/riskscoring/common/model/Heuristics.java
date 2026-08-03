package com.riskscoring.common.model;

public record Heuristics(
        Boolean freshWallet,
        Boolean fundedThenDrained,
        boolean roundAmounts,
        int fanIn,
        int fanOut
) {
}
