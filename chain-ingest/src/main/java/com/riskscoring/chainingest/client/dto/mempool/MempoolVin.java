package com.riskscoring.chainingest.client.dto.mempool;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MempoolVin(
        MempoolVout prevout,
        @JsonProperty("is_coinbase") boolean coinbase
) {
}
