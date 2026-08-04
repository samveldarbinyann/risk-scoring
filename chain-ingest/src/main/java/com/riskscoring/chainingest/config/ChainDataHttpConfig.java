package com.riskscoring.chainingest.config;

import com.riskscoring.chainingest.client.HttpCallTemplate;
import com.riskscoring.chainingest.client.impl.SlidingWindowRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class ChainDataHttpConfig {

    private static final String MORALIS = "Moralis";
    private static final String MEMPOOL = "mempool.space";
    private static final String HELIUS = "Helius";
    private static final String TRON_GRID = "TronGrid";
    private static final String TON_API = "TonAPI";

    private static final String MORALIS_API_KEY_HEADER = "X-API-Key";
    private static final String TRON_API_KEY_HEADER = "TRON-PRO-API-KEY";

    private final ChainIngestProperties properties;

    @Bean
    public HttpCallTemplate moralisCallTemplate() {
        return callTemplate(MORALIS, properties.moralis(), ApiKeyHeader.plain(MORALIS_API_KEY_HEADER));
    }

    @Bean
    public HttpCallTemplate mempoolCallTemplate() {
        return callTemplate(MEMPOOL, properties.mempool(), ApiKeyHeader.none());
    }

    @Bean
    public HttpCallTemplate heliusCallTemplate() {
        return callTemplate(HELIUS, properties.helius(), ApiKeyHeader.none());
    }

    @Bean
    public HttpCallTemplate tronGridCallTemplate() {
        return callTemplate(TRON_GRID, properties.tronGrid(), ApiKeyHeader.plain(TRON_API_KEY_HEADER));
    }

    @Bean
    public HttpCallTemplate tonApiCallTemplate() {
        return callTemplate(TON_API, properties.tonApi(), ApiKeyHeader.bearer());
    }

    private HttpCallTemplate callTemplate(String provider,
                                          ChainIngestProperties.Provider settings,
                                          ApiKeyHeader apiKeyHeader) {
        RestClient.Builder restClient = RestClient.builder()
                .baseUrl(settings.baseUrl())
                .requestFactory(ClientHttpRequestFactoryBuilder.jdk().build(HttpClientSettings.defaults()
                        .withTimeouts(settings.connectTimeout(), settings.readTimeout())));

        apiKeyHeader.value(settings.apiKey())
                .ifPresent(value -> restClient.defaultHeader(apiKeyHeader.name(), value));

        return new HttpCallTemplate(provider, restClient.build(),
                new SlidingWindowRateLimiter(settings.callsPerSecond()),
                settings.rateLimitRetries(), settings.rateLimitBackoff());
    }
}
