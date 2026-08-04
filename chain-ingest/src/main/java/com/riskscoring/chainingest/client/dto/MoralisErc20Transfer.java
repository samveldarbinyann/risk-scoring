package com.riskscoring.chainingest.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MoralisErc20Transfer(
        @JsonProperty("from_address") String fromAddress,
        @JsonProperty("to_address") String toAddress,
        @JsonProperty("address") String contract,
        @JsonProperty("token_symbol") String symbol,
        @JsonProperty("value_formatted") String valueFormatted
) {
}
