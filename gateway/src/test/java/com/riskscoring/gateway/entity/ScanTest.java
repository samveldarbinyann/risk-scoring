package com.riskscoring.gateway.entity;

import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ScanTarget;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScanTest {

    @Test
    void instancesWithSameNonNullIdAreEqual() {
        UUID id = UUID.randomUUID();
        Scan a = scan(id);
        Scan b = scan(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void instancesWithDifferentIdsAreNotEqual() {
        Scan a = scan(UUID.randomUUID());
        Scan b = scan(UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void instancesWithNullIdAreNeverEqualToEachOther() {
        Scan a = scan(null);
        Scan b = scan(null);

        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void isEqualToItself() {
        Scan a = scan(UUID.randomUUID());

        assertThat(a).isEqualTo(a);
    }

    @Test
    void isNotEqualToNullOrUnrelatedType() {
        Scan a = scan(UUID.randomUUID());

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not a Scan");
    }

    private static Scan scan(UUID id) {
        return Scan.builder()
                .id(id)
                .groupId(UUID.randomUUID())
                .targetType(ScanTarget.ADDRESS)
                .target("0xabc")
                .chain(Chain.ETHEREUM)
                .status(ScanStage.PENDING)
                .source(ScanSource.USER)
                .requestedAt(Instant.now())
                .build();
    }
}
