package com.riskscoring.chainingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "chain-ingest")
public record ChainIngestProperties(
        Duration cacheTtl,
        int maxCounterparties
) {
}