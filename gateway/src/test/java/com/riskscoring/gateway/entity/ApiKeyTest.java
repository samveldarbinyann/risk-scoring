package com.riskscoring.gateway.entity;

import com.riskscoring.gateway.model.ApiKeyStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyTest {

    @Test
    void instancesWithSameNonNullIdAreEqual() {
        UUID id = UUID.randomUUID();
        ApiKey a = apiKey(id);
        ApiKey b = apiKey(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void instancesWithDifferentIdsAreNotEqual() {
        ApiKey a = apiKey(UUID.randomUUID());
        ApiKey b = apiKey(UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void instancesWithNullIdAreNeverEqualToEachOther() {
        ApiKey a = apiKey(null);
        ApiKey b = apiKey(null);

        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void isEqualToItself() {
        ApiKey a = apiKey(UUID.randomUUID());

        assertThat(a).isEqualTo(a);
    }

    @Test
    void isNotEqualToNullOrUnrelatedType() {
        ApiKey a = apiKey(UUID.randomUUID());

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not an ApiKey");
    }

    private static ApiKey apiKey(UUID id) {
        return ApiKey.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .name("CI key")
                .keyPrefix("rsk_abc123")
                .keyHash("hash")
                .status(ApiKeyStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
    }
}
