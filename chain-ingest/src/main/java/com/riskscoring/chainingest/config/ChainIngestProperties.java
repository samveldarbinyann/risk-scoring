package com.riskscoring.chainingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "chain-ingest")
public record ChainIngestProperties(
        Duration cacheTtl,
        int maxCounterparties,
        int hop2ExpandTop,
        int hop2Reserve,
        int maxTokenBalances,
        int maxTokenTransfers,
        Provider moralis,
        Provider mempool,
        Provider helius,
        Provider tronGrid
) {

    public record Provider(
            String baseUrl,
            String apiKey,
            int pageSize,
            int maxHops,
            int callsPerSecond,
            int rateLimitRetries,
            Duration rateLimitBackoff,
            Duration connectTimeout,
            Duration readTimeout
    ) {
    }
}
