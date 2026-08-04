package com.riskscoring.chainingest.client.dto.helius;

public record HeliusNativeTransfer(
        String fromUserAccount,
        String toUserAccount,
        long amount
) {
}