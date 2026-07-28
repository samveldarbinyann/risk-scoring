package com.riskscoring.chainingest.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class MoralisRestClientConfig {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final ChainIngestProperties properties;

    @Bean
    public RestClient moralisRestClient() {
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.jdk()
                .build(HttpClientSettings.defaults()
                        .withTimeouts(properties.moralis().connectTimeout(),
                                properties.moralis().readTimeout()));

        return RestClient.builder()
                .baseUrl(properties.moralis().baseUrl())
                .defaultHeader(API_KEY_HEADER, properties.moralis().apiKey())
                .requestFactory(requestFactory)
                .build();
    }
}
