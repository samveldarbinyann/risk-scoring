package com.riskscoring.enrichment.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class OfacHttpConfig {

    private final EnrichmentProperties properties;

    @Bean
    public RestClient ofacRestClient() {
        EnrichmentProperties.Ofac settings = properties.ofac();

        return RestClient.builder()
                .baseUrl(settings.baseUrl())
                .requestFactory(ClientHttpRequestFactoryBuilder.jdk().build(HttpClientSettings.defaults()
                        .withTimeouts(settings.connectTimeout(), settings.readTimeout())))
                .build();
    }
}
