package com.riskscoring.common.model;

public record TokenBalance(
        String symbol,
        String balanceFormatted,
        Double usdValue
) {
}