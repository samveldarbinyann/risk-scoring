package com.riskscoring.chainingest.client.dto.moralis;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MoralisTxRef(
        @JsonProperty("block_number") String blockNumber,
        @JsonProperty("block_timestamp") String blockTimestamp,
        @JsonProperty("transaction_hash") String transactionHash
) {
}
