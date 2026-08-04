package com.riskscoring.common.model;

public record TokenTransfer(
        String symbol,
        String contract,
        String from,
        String to,
        String amount
) {
}
