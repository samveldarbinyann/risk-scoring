package com.riskscoring.gateway.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class SecretGeneratorTest {

    private final SecretGenerator generator = new SecretGenerator();

    @Test
    void generateReturnsThirtyTwoBytesUrlSafeBase64WithoutPadding() {
        String secret = generator.generate();

        assertThat(secret).doesNotContain("=");
        assertThat(secret).matches("[A-Za-z0-9_-]+");
        assertThat(Base64.getUrlDecoder().decode(secret)).hasSize(32);
    }

    @Test
    void generateWithLengthProducesRequestedByteLength() {
        String secret = generator.generate(16);

        assertThat(Base64.getUrlDecoder().decode(secret)).hasSize(16);
    }

    @Test
    void generateWithZeroLengthReturnsEmptyString() {
        assertThat(generator.generate(0)).isEmpty();
    }

    @Test
    void generateProducesDifferentValuesAcrossCalls() {
        assertThat(generator.generate()).isNotEqualTo(generator.generate());
    }
}
