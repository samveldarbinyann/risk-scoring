package com.riskscoring.chainingest.client.dto.helius;

public record HeliusRpcError(
        int code,
        String message
) {
}
