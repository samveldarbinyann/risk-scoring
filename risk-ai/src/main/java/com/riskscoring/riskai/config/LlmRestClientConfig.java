package com.riskscoring.riskai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@RequiredArgsConstructor
public class LlmRestClientConfig {

    private final RiskAiProperties properties;

    @Bean
    public RestClient llmRestClient() {
        JsonMapper snakeCaseMapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();

        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.jdk()
                .build(HttpClientSettings.defaults()
                        .withTimeouts(properties.llm().connectTimeout(), properties.llm().readTimeout()));

        return RestClient.builder()
                .baseUrl(properties.llm().baseUrl())
                .requestFactory(requestFactory)
                .messageConverters(converters -> {
                    converters.removeIf(JacksonJsonHttpMessageConverter.class::isInstance);
                    converters.add(new JacksonJsonHttpMessageConverter(snakeCaseMapper));
                })
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.llm().apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
