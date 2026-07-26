package com.riskscoring.common.model;

public record Heuristics(
        boolean freshWallet,
        boolean fundedThenDrained,
        boolean roundAmounts,
        int fanIn,
        int fanOut
) {
}
