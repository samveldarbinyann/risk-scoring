package com.riskscoring.paymentwatch.config;

import com.riskscoring.paymentwatch.client.HttpCallTemplate;
import com.riskscoring.paymentwatch.client.impl.SlidingWindowRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class MoralisHttpConfig {

    private static final String PROVIDER = "Moralis";
    private static final String API_KEY_HEADER = "X-API-Key";

    private final PaymentWatchProperties properties;

    @Bean
    public HttpCallTemplate moralisCallTemplate() {
        PaymentWatchProperties.Moralis settings = properties.moralis();

        RestClient restClient = RestClient.builder()
                .baseUrl(settings.baseUrl())
                .requestFactory(ClientHttpRequestFactoryBuilder.jdk().build(HttpClientSettings.defaults()
                        .withTimeouts(settings.connectTimeout(), settings.readTimeout())))
                .defaultHeader(API_KEY_HEADER, settings.apiKey())
                .build();

        return new HttpCallTemplate(PROVIDER, restClient,
                new SlidingWindowRateLimiter(settings.callsPerSecond()),
                settings.rateLimitRetries(), settings.rateLimitBackoff());
    }
}
