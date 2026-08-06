package com.riskscoring.chainingest.client.dto.moralis;

import java.util.List;

public record MoralisTokenBalancesEnvelope(
        List<MoralisTokenBalance> result
) {
}
