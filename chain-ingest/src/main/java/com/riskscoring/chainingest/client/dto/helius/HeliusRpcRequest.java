package com.riskscoring.chainingest.client.dto.helius;

public record HeliusRpcRequest(
        String jsonrpc,
        String id,
        String method,
        Object params
) {
}
