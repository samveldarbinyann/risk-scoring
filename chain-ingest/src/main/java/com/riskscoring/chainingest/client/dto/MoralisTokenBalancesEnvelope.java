package com.riskscoring.chainingest.client.dto;

import java.util.List;

public record MoralisTokenBalancesEnvelope(
        List<MoralisTokenBalance> result
) {
}
