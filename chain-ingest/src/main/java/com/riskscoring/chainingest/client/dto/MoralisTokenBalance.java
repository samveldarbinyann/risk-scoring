package com.riskscoring.chainingest.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MoralisTokenBalance(
        String symbol,
        String balance,
        @JsonProperty("balance_formatted") String balanceFormatted,
        int decimals,
        @JsonProperty("usd_value") Double usdValue,
        @JsonProperty("native_token") boolean nativeToken,
        @JsonProperty("possible_spam") boolean possibleSpam
) {
}