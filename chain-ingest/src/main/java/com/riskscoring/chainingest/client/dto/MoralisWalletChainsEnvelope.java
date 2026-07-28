package com.riskscoring.chainingest.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MoralisWalletChainsEnvelope(
        String address,
        @JsonProperty("active_chains") List<MoralisActiveChain> activeChains
) {
}
