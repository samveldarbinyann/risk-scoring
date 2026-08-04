package com.riskscoring.chainingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "chain-ingest")
public record ChainIngestProperties(
        Duration cacheTtl,
        int maxCounterparties,
        int maxHops,
        int hop2ExpandTop,
        int hop2Reserve,
        int maxTokenBalances,
        int maxTokenTransfers,
        Moralis moralis,
        Mempool mempool,
        Helius helius,
        TronGrid tronGrid
) {

    public record Moralis(
            String baseUrl,
            String apiKey,
            int pageSize,
            int callsPerSecond,
            int rateLimitRetries,
            Duration rateLimitBackoff,
            Duration connectTimeout,
            Duration readTimeout
    ) {
    }

    public record Mempool(
            String baseUrl,
            int maxHops,
            int callsPerSecond,
            int rateLimitRetries,
            Duration rateLimitBackoff,
            Duration connectTimeout,
            Duration readTimeout
    ) {
    }

    public record Helius(
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

    public record TronGrid(
            String baseUrl,
            String apiKey,
            int pageSize,
            int maxHops,
            Duration transferWindow,
            int callsPerSecond,
            int rateLimitRetries,
            Duration rateLimitBackoff,
            Duration connectTimeout,
            Duration readTimeout
    ) {
    }
}
