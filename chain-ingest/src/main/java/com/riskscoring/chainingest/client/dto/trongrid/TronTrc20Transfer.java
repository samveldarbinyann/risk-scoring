package com.riskscoring.chainingest.client.dto.trongrid;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TronTrc20Transfer(
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("block_timestamp") long blockTimestamp,
        String from,
        String to,
        String value,
        String type,
        @JsonProperty("token_info") TronTokenInfo tokenInfo
) {
}
