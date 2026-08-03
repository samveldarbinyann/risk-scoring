package com.riskscoring.chainingest.client.dto.mempool;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MempoolAddressStats(
        String address,
        @JsonProperty("chain_stats") MempoolChainStats chainStats,
        @JsonProperty("mempool_stats") MempoolChainStats mempoolStats
) {
}
