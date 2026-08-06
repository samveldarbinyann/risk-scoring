package com.riskscoring.chainingest.client.dto.moralis;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MoralisTokenBalance(
        String symbol,
        @JsonProperty("balance_formatted") String balanceFormatted,
        @JsonProperty("usd_value") Double usdValue
) {
}
