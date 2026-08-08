package com.riskscoring.paymentwatch.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MoralisTokenTransfer(
        @JsonProperty("transaction_hash") String transactionHash,
        @JsonProperty("block_number") String blockNumber,
        @JsonProperty("block_timestamp") String blockTimestamp,
        @JsonProperty("address") String contract,
        @JsonProperty("from_address") String fromAddress,
        @JsonProperty("to_address") String toAddress,
        @JsonProperty("value_decimal") String valueDecimal
) {
}
