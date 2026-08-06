package com.riskscoring.gateway.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretHasherTest {

    private final SecretHasher hasher = new SecretHasher("pepper-1234567890123456");

    @Test
    void hashIsDeterministicForSameSecretAndPepper() {
        assertThat(hasher.hash("secret")).isEqualTo(hasher.hash("secret"));
    }

    @Test
    void hashProducesSixtyFourLowercaseHexCharacters() {
        String hash = hasher.hash("secret");

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void hashDiffersForDifferentPeppers() {
        SecretHasher other = new SecretHasher("different-pepper-123456");

        assertThat(hasher.hash("secret")).isNotEqualTo(other.hash("secret"));
    }

    @Test
    void hashDiffersForDifferentSecrets() {
        assertThat(hasher.hash("secret-a")).isNotEqualTo(hasher.hash("secret-b"));
    }

    @Test
    void matchesReturnsTrueForCorrectSecret() {
        String hash = hasher.hash("secret");

        assertThat(hasher.matches("secret", hash)).isTrue();
    }

    @Test
    void matchesReturnsFalseForWrongSecret() {
        String hash = hasher.hash("secret");

        assertThat(hasher.matches("other-secret", hash)).isFalse();
    }

    @Test
    void matchesReturnsFalseWhenExpectedHashCaseDiffers() {
        String hash = hasher.hash("secret");

        assertThat(hasher.matches("secret", hash.toUpperCase())).isFalse();
    }

    @Test
    void matchesHandlesEmptySecret() {
        assertThat(hasher.matches("", hasher.hash(""))).isTrue();
    }
}
