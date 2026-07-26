package com.riskscoring.common.model;

public record Counterparty(
        String address,
        TransferDirection direction,
        long txCount,
        String totalValueWei,
        int hops
) {
}