package com.riskscoring.chainingest.client.dto.tonapi;

public record TonJettonTransferAction(
        TonAccountAddress sender,
        TonAccountAddress recipient,
        String amount,
        TonJettonPreview jetton
) {
}
