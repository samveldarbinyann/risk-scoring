package com.riskscoring.chainingest.client.dto.helius;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HeliusTokenInfo(
        String symbol,
        String balance,
        int decimals,
        @JsonProperty("price_info") HeliusPriceInfo priceInfo
) {
}
