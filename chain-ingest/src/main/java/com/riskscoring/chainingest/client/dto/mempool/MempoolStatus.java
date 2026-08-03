package com.riskscoring.chainingest.client.dto.mempool;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MempoolStatus(
        boolean confirmed,
        @JsonProperty("block_height") Long blockHeight,
        @JsonProperty("block_time") Long blockTime
) {
}
