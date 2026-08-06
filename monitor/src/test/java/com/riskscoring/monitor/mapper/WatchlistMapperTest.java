package com.riskscoring.monitor.mapper;

import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.event.WatchlistAddRequested;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.monitor.entity.WatchlistEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WatchlistMapperTest {

    private static final Instant NOW = Instant.parse("2024-06-01T00:00:00Z");

    private final WatchlistMapper mapper = new WatchlistMapper();

    @Test
    void toEntityMapsFieldsFromEventAndActivatesEntry() {
        WatchlistAddRequested event = new WatchlistAddRequested(
                UUID.randomUUID(), UUID.randomUUID(), "0xabc", Chain.ETHEREUM, Language.RU, Instant.now());

        WatchlistEntry entry = mapper.toEntity(event, NOW);

        assertThat(entry.getId()).isNotNull();
        assertThat(entry.getUserId()).isEqualTo(event.userId());
        assertThat(entry.getAddress()).isEqualTo(event.address());
        assertThat(entry.getChain()).isEqualTo(event.chain());
        assertThat(entry.getLanguage()).isEqualTo(event.language());
        assertThat(entry.isActive()).isTrue();
        assertThat(entry.getCreatedAt()).isEqualTo(NOW);
        assertThat(entry.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void toEntityGeneratesDistinctRandomIdsAcrossCalls() {
        WatchlistAddRequested event = new WatchlistAddRequested(
                UUID.randomUUID(), UUID.randomUUID(), "0xabc", Chain.ETHEREUM, Language.EN, Instant.now());

        WatchlistEntry first = mapper.toEntity(event, NOW);
        WatchlistEntry second = mapper.toEntity(event, NOW);

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void toScanRequestedUsesAddressTargetTypeAndMonitorSource() {
        WatchlistEntry entry = WatchlistEntry.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .address("0xabc")
                .chain(Chain.ETHEREUM)
                .language(Language.RU)
                .active(true)
                .build();
        UUID scanId = UUID.randomUUID();

        ScanRequested scanRequested = mapper.toScanRequested(entry, scanId, NOW);

        assertThat(scanRequested).isEqualTo(new ScanRequested(
                scanId, ScanTarget.ADDRESS, entry.getAddress(), entry.getChain(),
                NOW, ScanSource.MONITOR, entry.getLanguage(), entry.getUserId()));
    }
}
