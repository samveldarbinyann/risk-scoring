package com.riskscoring.chainingest.client.dto.mempool;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MempoolChainStats(
        @JsonProperty("funded_txo_count") long fundedTxoCount,
        @JsonProperty("funded_txo_sum") long fundedTxoSum,
        @JsonProperty("spent_txo_count") long spentTxoCount,
        @JsonProperty("spent_txo_sum") long spentTxoSum,
        @JsonProperty("tx_count") long txCount
) {
}
