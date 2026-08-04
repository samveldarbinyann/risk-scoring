package com.riskscoring.chainingest.client.dto.helius;

public record HeliusTokenTransfer(
        String fromUserAccount,
        String toUserAccount,
        String mint,
        double tokenAmount
) {
}