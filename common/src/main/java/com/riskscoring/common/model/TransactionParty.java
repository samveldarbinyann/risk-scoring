package com.riskscoring.common.model;

public record TransactionParty(
        String address,
        TransactionRole role,
        String valueNative
) {
}
