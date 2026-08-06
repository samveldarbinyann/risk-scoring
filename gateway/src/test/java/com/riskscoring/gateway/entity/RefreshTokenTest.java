package com.riskscoring.gateway.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    void instancesWithSameNonNullIdAreEqual() {
        UUID id = UUID.randomUUID();
        RefreshToken a = token(id);
        RefreshToken b = token(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void instancesWithDifferentIdsAreNotEqual() {
        RefreshToken a = token(UUID.randomUUID());
        RefreshToken b = token(UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void instancesWithNullIdAreNeverEqualToEachOther() {
        RefreshToken a = token(null);
        RefreshToken b = token(null);

        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void isEqualToItself() {
        RefreshToken a = token(UUID.randomUUID());

        assertThat(a).isEqualTo(a);
    }

    @Test
    void isNotEqualToNullOrUnrelatedType() {
        RefreshToken a = token(UUID.randomUUID());

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not a RefreshToken");
    }

    private static RefreshToken token(UUID id) {
        Instant now = Instant.now();
        return RefreshToken.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .tokenHash("hash")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .userAgent("junit")
                .ipAddress("203.0.113.10")
                .build();
    }
}
