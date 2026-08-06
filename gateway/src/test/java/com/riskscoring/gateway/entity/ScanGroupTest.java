package com.riskscoring.gateway.entity;

import com.riskscoring.common.model.ScanTarget;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScanGroupTest {

    @Test
    void instancesWithSameNonNullIdAreEqual() {
        UUID id = UUID.randomUUID();
        ScanGroup a = scanGroup(id);
        ScanGroup b = scanGroup(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void instancesWithDifferentIdsAreNotEqual() {
        ScanGroup a = scanGroup(UUID.randomUUID());
        ScanGroup b = scanGroup(UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void instancesWithNullIdAreNeverEqualToEachOther() {
        ScanGroup a = scanGroup(null);
        ScanGroup b = scanGroup(null);

        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void isEqualToItself() {
        ScanGroup a = scanGroup(UUID.randomUUID());

        assertThat(a).isEqualTo(a);
    }

    @Test
    void isNotEqualToNullOrUnrelatedType() {
        ScanGroup a = scanGroup(UUID.randomUUID());

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not a ScanGroup");
    }

    private static ScanGroup scanGroup(UUID id) {
        return ScanGroup.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .targetType(ScanTarget.ADDRESS)
                .target("0xabc")
                .requestedAt(Instant.now())
                .build();
    }
}
