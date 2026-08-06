package com.riskscoring.chainingest.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyHeaderTest {

    @Test
    void plainFormatsValueAsIs() {
        ApiKeyHeader header = ApiKeyHeader.plain("X-API-Key");

        assertThat(header.name()).isEqualTo("X-API-Key");
        assertThat(header.value("secret")).contains("secret");
    }

    @Test
    void bearerUsesAuthorizationHeaderWithBearerPrefix() {
        ApiKeyHeader header = ApiKeyHeader.bearer();

        assertThat(header.name()).isEqualTo(HttpHeaders.AUTHORIZATION);
        assertThat(header.value("secret")).contains("Bearer secret");
    }

    @Test
    void noneNeverProducesAValue() {
        ApiKeyHeader header = ApiKeyHeader.none();

        assertThat(header.name()).isNull();
        assertThat(header.value("secret")).isEmpty();
        assertThat(header.value(null)).isEmpty();
    }

    @Test
    void blankOrNullApiKeyProducesNoValueEvenForNamedHeaders() {
        ApiKeyHeader plain = ApiKeyHeader.plain("X-API-Key");
        ApiKeyHeader bearer = ApiKeyHeader.bearer();

        assertThat(plain.value(null)).isEqualTo(Optional.empty());
        assertThat(plain.value("")).isEqualTo(Optional.empty());
        assertThat(plain.value("   ")).isEqualTo(Optional.empty());
        assertThat(bearer.value("   ")).isEqualTo(Optional.empty());
    }
}
