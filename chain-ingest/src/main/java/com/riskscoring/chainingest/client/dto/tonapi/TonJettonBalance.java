package com.riskscoring.chainingest.client.dto.tonapi;

public record TonJettonBalance(
        String balance,
        TonJettonPreview jetton,
        TonPrice price
) {
}
