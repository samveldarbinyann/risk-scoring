package com.riskscoring.monitor.entity;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WatchlistEntryTest {

    @Test
    void instancesWithSameNonNullIdAreEqual() {
        UUID id = UUID.randomUUID();
        WatchlistEntry a = entry(id);
        WatchlistEntry b = entry(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void instancesWithDifferentIdsAreNotEqual() {
        WatchlistEntry a = entry(UUID.randomUUID());
        WatchlistEntry b = entry(UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void instancesWithNullIdAreNeverEqualToEachOther() {
        WatchlistEntry a = entry(null);
        WatchlistEntry b = entry(null);

        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void isEqualToItself() {
        WatchlistEntry a = entry(UUID.randomUUID());

        assertThat(a).isEqualTo(a);
    }

    @Test
    void isNotEqualToNullOrUnrelatedType() {
        WatchlistEntry a = entry(UUID.randomUUID());

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not a WatchlistEntry");
    }

    private static WatchlistEntry entry(UUID id) {
        Instant now = Instant.now();
        return WatchlistEntry.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .address("0xabc")
                .chain(Chain.ETHEREUM)
                .language(Language.EN)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
