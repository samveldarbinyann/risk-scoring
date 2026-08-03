package com.riskscoring.chainingest.config;

import com.riskscoring.chainingest.client.HttpCallTemplate;
import com.riskscoring.chainingest.client.impl.SlidingWindowRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class ChainDataHttpConfig {

    private static final String MORALIS = "Moralis";
    private static final String MEMPOOL = "mempool.space";
    private static final String API_KEY_HEADER = "X-API-Key";

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

    private ClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        return ClientHttpRequestFactoryBuilder.jdk()
                .build(HttpClientSettings.defaults().withTimeouts(connectTimeout, readTimeout));
    }
}
