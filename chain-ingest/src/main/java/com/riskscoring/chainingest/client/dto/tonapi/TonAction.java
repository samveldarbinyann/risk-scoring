package com.riskscoring.chainingest.client.dto.tonapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TonAction(
        String type,
        String status,
        @JsonProperty("TonTransfer") TonTransferAction tonTransfer,
        @JsonProperty("JettonTransfer") TonJettonTransferAction jettonTransfer
) {
}
