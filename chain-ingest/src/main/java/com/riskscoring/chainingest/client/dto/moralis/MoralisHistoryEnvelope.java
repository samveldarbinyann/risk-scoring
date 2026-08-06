package com.riskscoring.chainingest.client.dto.moralis;

import java.util.List;

public record MoralisHistoryEnvelope(
        String cursor,
        List<MoralisTransaction> result
) {
}
