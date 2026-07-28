package com.riskscoring.chainingest.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MoralisHistoryEnvelope(
        String cursor,
        int page,
        @JsonProperty("page_size") int pageSize,
        List<MoralisTransaction> result
) {
}
