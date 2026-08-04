package com.riskscoring.chainingest.client.dto.helius;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HeliusAsset(
        @JsonProperty("token_info") HeliusTokenInfo tokenInfo
) {
}
