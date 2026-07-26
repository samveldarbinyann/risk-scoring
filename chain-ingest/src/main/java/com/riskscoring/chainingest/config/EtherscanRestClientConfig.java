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
public class EtherscanRestClientConfig {

    private final ChainIngestProperties properties;

    @Bean
    public RestClient etherscanRestClient() {
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.jdk()
                .build(HttpClientSettings.defaults()
                        .withTimeouts(properties.etherscan().connectTimeout(),
                                properties.etherscan().readTimeout()));

        return RestClient.builder()
                .baseUrl(properties.etherscan().baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
