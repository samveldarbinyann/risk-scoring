package com.riskscoring.enrichment.entity;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ScanTarget;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceRecordTest {

    @Test
    void instancesWithSameNonNullIdAreEqual() {
        UUID id = UUID.randomUUID();
        EvidenceRecord a = record(id);
        EvidenceRecord b = record(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void instancesWithDifferentIdsAreNotEqual() {
        EvidenceRecord a = record(UUID.randomUUID());
        EvidenceRecord b = record(UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void instancesWithNullIdAreNeverEqualToEachOther() {
        EvidenceRecord a = record(null);
        EvidenceRecord b = record(null);

        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void isEqualToItself() {
        EvidenceRecord a = record(UUID.randomUUID());

        assertThat(a).isEqualTo(a);
    }

    @Test
    void isNotEqualToNullOrUnrelatedType() {
        EvidenceRecord a = record(UUID.randomUUID());

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not an EvidenceRecord");
    }

    private static EvidenceRecord record(UUID id) {
        return EvidenceRecord.builder()
                .id(id)
                .scanId(UUID.randomUUID())
                .targetType(ScanTarget.ADDRESS)
                .target("0xabc")
                .chain(Chain.ETHEREUM)
                .payload("{}")
                .createdAt(Instant.now())
                .build();
    }
}
