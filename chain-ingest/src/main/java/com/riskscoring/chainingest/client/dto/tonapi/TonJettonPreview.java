package com.riskscoring.chainingest.client.dto.tonapi;

public record TonJettonPreview(
        String address,
        String symbol,
        int decimals
) {
}
