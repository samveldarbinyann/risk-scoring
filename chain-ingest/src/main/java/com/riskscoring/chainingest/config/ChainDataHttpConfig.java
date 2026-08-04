package com.riskscoring.chainingest.config;

import com.riskscoring.chainingest.client.HttpCallTemplate;
import com.riskscoring.chainingest.client.impl.SlidingWindowRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class ChainDataHttpConfig {

    private static final String MORALIS = "Moralis";
    private static final String MEMPOOL = "mempool.space";
    private static final String HELIUS = "Helius";
    private static final String TRON_GRID = "TronGrid";
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String TRON_API_KEY_HEADER = "TRON-PRO-API-KEY";

    private final ChainIngestProperties properties;

    @Bean
    public HttpCallTemplate moralisCallTemplate() {
        ChainIngestProperties.Moralis moralis = properties.moralis();

        RestClient restClient = RestClient.builder()
                .baseUrl(moralis.baseUrl())
                .defaultHeader(API_KEY_HEADER, moralis.apiKey())
                .requestFactory(requestFactory(moralis.connectTimeout(), moralis.readTimeout()))
                .build();

        return new HttpCallTemplate(MORALIS, restClient, new SlidingWindowRateLimiter(moralis.callsPerSecond()),
                moralis.rateLimitRetries(), moralis.rateLimitBackoff());
    }

    @Bean
    public HttpCallTemplate mempoolCallTemplate() {
        ChainIngestProperties.Mempool mempool = properties.mempool();

        RestClient restClient = RestClient.builder()
                .baseUrl(mempool.baseUrl())
                .requestFactory(requestFactory(mempool.connectTimeout(), mempool.readTimeout()))
                .build();

        return new HttpCallTemplate(MEMPOOL, restClient, new SlidingWindowRateLimiter(mempool.callsPerSecond()),
                mempool.rateLimitRetries(), mempool.rateLimitBackoff());
    }

    @Bean
    public HttpCallTemplate heliusCallTemplate() {
        ChainIngestProperties.Helius helius = properties.helius();

        RestClient restClient = RestClient.builder()
                .baseUrl(helius.baseUrl())
                .requestFactory(requestFactory(helius.connectTimeout(), helius.readTimeout()))
                .build();

        return new HttpCallTemplate(HELIUS, restClient, new SlidingWindowRateLimiter(helius.callsPerSecond()),
                helius.rateLimitRetries(), helius.rateLimitBackoff());
    }

    @Bean
    public HttpCallTemplate tronGridCallTemplate() {
        ChainIngestProperties.TronGrid tronGrid = properties.tronGrid();

        RestClient.Builder restClient = RestClient.builder()
                .baseUrl(tronGrid.baseUrl())
                .requestFactory(requestFactory(tronGrid.connectTimeout(), tronGrid.readTimeout()));

        if (StringUtils.hasText(tronGrid.apiKey())) {
            restClient.defaultHeader(TRON_API_KEY_HEADER, tronGrid.apiKey());
        }

        return new HttpCallTemplate(TRON_GRID, restClient.build(), new SlidingWindowRateLimiter(tronGrid.callsPerSecond()),
                tronGrid.rateLimitRetries(), tronGrid.rateLimitBackoff());
    }

    private ClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        return ClientHttpRequestFactoryBuilder.jdk()
                .build(HttpClientSettings.defaults().withTimeouts(connectTimeout, readTimeout));
    }
}
