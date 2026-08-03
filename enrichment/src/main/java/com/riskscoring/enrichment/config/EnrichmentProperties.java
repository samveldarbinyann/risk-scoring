package com.riskscoring.enrichment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "enrichment")
public record EnrichmentProperties(
        int freshWalletDays,
        int roundAmountsPercentThreshold,
        int internalFanOutThreshold
) {
}