package com.riskscoring.gateway.entity;

import com.riskscoring.gateway.model.EmailCodePurpose;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationCodeTest {

    @Test
    void instancesWithSameNonNullIdAreEqual() {
        UUID id = UUID.randomUUID();
        EmailVerificationCode a = code(id);
        EmailVerificationCode b = code(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void instancesWithDifferentIdsAreNotEqual() {
        EmailVerificationCode a = code(UUID.randomUUID());
        EmailVerificationCode b = code(UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void instancesWithNullIdAreNeverEqualToEachOther() {
        EmailVerificationCode a = code(null);
        EmailVerificationCode b = code(null);

        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void isEqualToItself() {
        EmailVerificationCode a = code(UUID.randomUUID());

        assertThat(a).isEqualTo(a);
    }

    @Test
    void isNotEqualToNullOrUnrelatedType() {
        EmailVerificationCode a = code(UUID.randomUUID());

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not an EmailVerificationCode");
    }

    private static EmailVerificationCode code(UUID id) {
        Instant now = Instant.now();
        return EmailVerificationCode.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .codeHash("hash")
                .purpose(EmailCodePurpose.REGISTRATION)
                .expiresAt(now.plusSeconds(600))
                .attempts(0)
                .createdAt(now)
                .build();
    }
}
