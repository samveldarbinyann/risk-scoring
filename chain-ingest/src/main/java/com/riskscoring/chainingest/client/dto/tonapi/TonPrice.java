package com.riskscoring.chainingest.client.dto.tonapi;

import java.util.Map;

public record TonPrice(
        Map<String, Double> prices
) {
}
