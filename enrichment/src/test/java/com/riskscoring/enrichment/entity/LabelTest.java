package com.riskscoring.enrichment.entity;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.LabelCategory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LabelTest {

    @Test
    void instancesWithSameNonNullIdAreEqual() {
        UUID id = UUID.randomUUID();
        Label a = label(id);
        Label b = label(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void instancesWithDifferentIdsAreNotEqual() {
        Label a = label(UUID.randomUUID());
        Label b = label(UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void instancesWithNullIdAreNeverEqualToEachOther() {
        Label a = label(null);
        Label b = label(null);

        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void isEqualToItself() {
        Label a = label(UUID.randomUUID());

        assertThat(a).isEqualTo(a);
    }

    @Test
    void isNotEqualToNullOrUnrelatedType() {
        Label a = label(UUID.randomUUID());

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not a Label");
    }

    private static Label label(UUID id) {
        return Label.builder()
                .id(id)
                .chain(Chain.ETHEREUM)
                .address("0xabc")
                .category(LabelCategory.SANCTION)
                .name("Test Label")
                .source("Test Source")
                .updatedAt(Instant.now())
                .build();
    }
}
