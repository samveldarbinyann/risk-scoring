package com.riskscoring.chainingest.client.dto.tonapi;

public record TonTransferAction(
        TonAccountAddress sender,
        TonAccountAddress recipient,
        Long amount
) {
}
