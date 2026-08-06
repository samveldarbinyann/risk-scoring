package com.riskscoring.riskai.entity;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.common.model.ScanTarget;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScanReportTest {

    @Test
    void instancesWithSameNonNullIdAreEqual() {
        UUID id = UUID.randomUUID();
        ScanReport a = report(id);
        ScanReport b = report(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void instancesWithDifferentIdsAreNotEqual() {
        ScanReport a = report(UUID.randomUUID());
        ScanReport b = report(UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void instancesWithNullIdAreNeverEqualToEachOther() {
        ScanReport a = report(null);
        ScanReport b = report(null);

        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void isEqualToItself() {
        ScanReport a = report(UUID.randomUUID());

        assertThat(a).isEqualTo(a);
    }

    @Test
    void isNotEqualToNullOrUnrelatedType() {
        ScanReport a = report(UUID.randomUUID());

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not a ScanReport");
    }

    private static ScanReport report(UUID id) {
        return ScanReport.builder()
                .id(id)
                .scanId(UUID.randomUUID())
                .targetType(ScanTarget.ADDRESS)
                .target("0xabc")
                .chain(Chain.ETHEREUM)
                .riskLevel(RiskLevel.LOW)
                .score(10)
                .explanation("clean wallet")
                .decisiveSignals("[]")
                .manualChecks("[]")
                .observedAt(Instant.now())
                .evidence("{}")
                .model("deepseek-chat")
                .promptVersion("v1")
                .createdAt(Instant.now())
                .build();
    }
}
