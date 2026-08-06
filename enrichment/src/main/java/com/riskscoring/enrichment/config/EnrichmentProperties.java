package com.riskscoring.enrichment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "enrichment")
public record EnrichmentProperties(
        int freshWalletDays,
        int roundAmountsPercentThreshold,
        int internalFanOutThreshold,
        Ofac ofac
) {

    public record Ofac(
            String baseUrl,
            String syncCron,
            Duration connectTimeout,
            Duration readTimeout
    ) {
    }
}