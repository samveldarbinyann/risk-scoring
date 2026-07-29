package com.riskscoring.chainingest.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MoralisActiveChain(
        String chain,
        @JsonProperty("chain_id") String chainId,
        @JsonProperty("first_transaction") MoralisTxRef firstTransaction,
        @JsonProperty("last_transaction") MoralisTxRef lastTransaction
) {
}
