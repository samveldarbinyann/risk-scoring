package com.riskscoring.chainingest.config;

import org.springframework.http.HttpHeaders;

import java.util.Optional;

public record ApiKeyHeader(String name, String valueFormat) {

    private static final String RAW_VALUE = "%s";
    private static final String BEARER_VALUE = "Bearer %s";

    public static ApiKeyHeader plain(String name) {
        return new ApiKeyHeader(name, RAW_VALUE);
    }

    public static ApiKeyHeader bearer() {
        return new ApiKeyHeader(HttpHeaders.AUTHORIZATION, BEARER_VALUE);
    }

    public static ApiKeyHeader none() {
        return new ApiKeyHeader(null, RAW_VALUE);
    }

    public Optional<String> value(String apiKey) {
        return Optional.ofNullable(name)
                .filter(ignored -> apiKey != null && !apiKey.isBlank())
                .map(ignored -> valueFormat.formatted(apiKey));
    }
}
