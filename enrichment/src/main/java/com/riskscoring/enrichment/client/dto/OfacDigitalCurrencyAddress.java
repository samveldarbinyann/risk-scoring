package com.riskscoring.enrichment.client.dto;

public record OfacDigitalCurrencyAddress(
        String ticker,
        String address,
        String entityName
) {
}
