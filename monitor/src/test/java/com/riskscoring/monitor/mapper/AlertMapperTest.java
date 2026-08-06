package com.riskscoring.monitor.mapper;

import com.riskscoring.common.event.AlertTriggered;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.monitor.entity.Alert;
import com.riskscoring.monitor.entity.WatchlistEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AlertMapperTest {

    private static final Instant TRIGGERED_AT = Instant.parse("2024-06-01T00:00:00Z");

    private final AlertMapper mapper = new AlertMapper();

    @Test
    void toEntityMapsIdentityAndPreviousRiskFromWatchlistEntry() {
        WatchlistEntry entry = watchlistEntry();
        UUID scanId = UUID.randomUUID();

        Alert alert = mapper.toEntity(entry, RiskLevel.HIGH, 70, scanId, TRIGGERED_AT);

        assertThat(alert.getWatchlistEntryId()).isEqualTo(entry.getId());
        assertThat(alert.getUserId()).isEqualTo(entry.getUserId());
        assertThat(alert.getAddress()).isEqualTo(entry.getAddress());
        assertThat(alert.getChain()).isEqualTo(entry.getChain());
        assertThat(alert.getPreviousRiskLevel()).isEqualTo(entry.getLastRiskLevel());
        assertThat(alert.getPreviousScore()).isEqualTo(entry.getLastScore());
    }

    @Test
    void toEntityMapsNewRiskScanIdAndTriggeredAtFromArguments() {
        WatchlistEntry entry = watchlistEntry();
        UUID scanId = UUID.randomUUID();

        Alert alert = mapper.toEntity(entry, RiskLevel.HIGH, 70, scanId, TRIGGERED_AT);

        assertThat(alert.getId()).isNotNull();
        assertThat(alert.getNewRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(alert.getNewScore()).isEqualTo(70);
        assertThat(alert.getScanId()).isEqualTo(scanId);
        assertThat(alert.getTriggeredAt()).isEqualTo(TRIGGERED_AT);
    }

    @Test
    void toEntityGeneratesDistinctRandomIdsAcrossCalls() {
        WatchlistEntry entry = watchlistEntry();

        Alert first = mapper.toEntity(entry, RiskLevel.HIGH, 70, UUID.randomUUID(), TRIGGERED_AT);
        Alert second = mapper.toEntity(entry, RiskLevel.HIGH, 70, UUID.randomUUID(), TRIGGERED_AT);

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void toEventMapsAllFieldsFromAlert() {
        Alert alert = Alert.builder()
                .id(UUID.randomUUID())
                .watchlistEntryId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .address("0xabc")
                .chain(Chain.ETHEREUM)
                .previousRiskLevel(RiskLevel.LOW)
                .previousScore(10)
                .newRiskLevel(RiskLevel.HIGH)
                .newScore(70)
                .scanId(UUID.randomUUID())
                .triggeredAt(TRIGGERED_AT)
                .build();

        AlertTriggered event = mapper.toEvent(alert);

        assertThat(event).isEqualTo(new AlertTriggered(
                alert.getId(), alert.getWatchlistEntryId(), alert.getUserId(), alert.getAddress(), alert.getChain(),
                alert.getPreviousRiskLevel(), alert.getPreviousScore(), alert.getNewRiskLevel(), alert.getNewScore(),
                alert.getScanId(), alert.getTriggeredAt()));
    }

    private static WatchlistEntry watchlistEntry() {
        return WatchlistEntry.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .address("0xabc")
                .chain(Chain.ETHEREUM)
                .lastRiskLevel(RiskLevel.LOW)
                .lastScore(10)
                .build();
    }
}
