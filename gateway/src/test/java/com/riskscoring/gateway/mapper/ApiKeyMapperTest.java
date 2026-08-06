package com.riskscoring.gateway.mapper;

import com.riskscoring.gateway.entity.ApiKey;
import com.riskscoring.gateway.model.ApiKeyStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyMapperTest {

    private final ApiKeyMapper mapper = new ApiKeyMapper();

    @Test
    void toViewMapsFieldsWithoutExposingKeyHash() {
        ApiKey apiKey = apiKey();

        var view = mapper.toView(apiKey);

        assertThat(view.id()).isEqualTo(apiKey.getId());
        assertThat(view.name()).isEqualTo(apiKey.getName());
        assertThat(view.keyPrefix()).isEqualTo(apiKey.getKeyPrefix());
        assertThat(view.status()).isEqualTo(apiKey.getStatus());
        assertThat(view.createdAt()).isEqualTo(apiKey.getCreatedAt());
        assertThat(view.revokedAt()).isEqualTo(apiKey.getRevokedAt());
    }

    @Test
    void toCreatedViewUsesGivenPlaintextRatherThanAnyEntityField() {
        ApiKey apiKey = apiKey();

        var view = mapper.toCreatedView(apiKey, "rsk_abc123_plaintext");

        assertThat(view.apiKey()).isEqualTo("rsk_abc123_plaintext");
        assertThat(view.id()).isEqualTo(apiKey.getId());
        assertThat(view.keyPrefix()).isEqualTo(apiKey.getKeyPrefix());
        assertThat(view.status()).isEqualTo(apiKey.getStatus());
    }

    private static ApiKey apiKey() {
        return ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .name("CI key")
                .keyPrefix("rsk_abc123")
                .keyHash("hash")
                .status(ApiKeyStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
    }
}
