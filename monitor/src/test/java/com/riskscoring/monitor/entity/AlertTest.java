package com.riskscoring.monitor.entity;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.RiskLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AlertTest {

    @Test
    void instancesWithSameNonNullIdAreEqual() {
        UUID id = UUID.randomUUID();
        Alert a = alert(id);
        Alert b = alert(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void instancesWithDifferentIdsAreNotEqual() {
        Alert a = alert(UUID.randomUUID());
        Alert b = alert(UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void instancesWithNullIdAreNeverEqualToEachOther() {
        Alert a = alert(null);
        Alert b = alert(null);

        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void isEqualToItself() {
        Alert a = alert(UUID.randomUUID());

        assertThat(a).isEqualTo(a);
    }

    @Test
    void isNotEqualToNullOrUnrelatedType() {
        Alert a = alert(UUID.randomUUID());

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not an Alert");
    }

    private static Alert alert(UUID id) {
        return Alert.builder()
                .id(id)
                .watchlistEntryId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .address("0xabc")
                .chain(Chain.ETHEREUM)
                .previousRiskLevel(RiskLevel.LOW)
                .previousScore(10)
                .newRiskLevel(RiskLevel.HIGH)
                .newScore(70)
                .scanId(UUID.randomUUID())
                .triggeredAt(Instant.now())
                .build();
    }
}
