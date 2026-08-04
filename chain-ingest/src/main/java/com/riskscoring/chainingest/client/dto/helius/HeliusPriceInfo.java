package com.riskscoring.chainingest.client.dto.helius;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HeliusPriceInfo(
        @JsonProperty("total_price") Double totalPrice
) {
}
