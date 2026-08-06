package com.riskscoring.gateway.entity;

import com.riskscoring.common.model.Language;
import com.riskscoring.gateway.model.UserRole;
import com.riskscoring.gateway.model.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AppUserTest {

    @Test
    void instancesWithSameNonNullIdAreEqual() {
        UUID id = UUID.randomUUID();
        AppUser a = appUser(id);
        AppUser b = appUser(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void instancesWithDifferentIdsAreNotEqual() {
        AppUser a = appUser(UUID.randomUUID());
        AppUser b = appUser(UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void instancesWithNullIdAreNeverEqualToEachOther() {
        AppUser a = appUser(null);
        AppUser b = appUser(null);

        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void isEqualToItself() {
        AppUser a = appUser(UUID.randomUUID());

        assertThat(a).isEqualTo(a);
    }

    @Test
    void isNotEqualToNullOrUnrelatedType() {
        AppUser a = appUser(UUID.randomUUID());

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not an AppUser");
    }

    private static AppUser appUser(UUID id) {
        Instant now = Instant.now();
        return AppUser.builder()
                .id(id)
                .username("user")
                .email("user@example.com")
                .passwordHash("hash")
                .firstName("Jane")
                .lastName("Doe")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .tokenVersion(0)
                .failedLoginAttempts(0)
                .language(Language.EN)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
