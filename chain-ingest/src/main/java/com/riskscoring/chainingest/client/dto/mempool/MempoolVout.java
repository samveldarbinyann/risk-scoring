package com.riskscoring.chainingest.client.dto.mempool;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MempoolVout(
        @JsonProperty("scriptpubkey_address") String address,
        long value
) {
}
