package com.riskscoring.chainingest.client.dto;

import java.util.List;

public record MoralisHistoryEnvelope(
        String cursor,
        List<MoralisTransaction> result
) {
}
