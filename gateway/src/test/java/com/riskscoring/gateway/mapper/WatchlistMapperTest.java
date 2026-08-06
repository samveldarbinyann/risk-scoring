package com.riskscoring.gateway.mapper;

import com.riskscoring.common.event.WatchlistAddRequested;
import com.riskscoring.common.event.WatchlistRemoveRequested;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.gateway.dto.WatchlistEntryView;
import com.riskscoring.gateway.repository.WatchlistEntryRow;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WatchlistMapperTest {

    private final WatchlistMapper mapper = new WatchlistMapper();

    @Test
    void toAddRequestedCarriesThroughGivenFieldsWithFreshIdAndTimestamp() {
        UUID userId = UUID.randomUUID();

        WatchlistAddRequested event = mapper.toAddRequested(userId, "0xabc", Chain.ETHEREUM, Language.RU);

        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.address()).isEqualTo("0xabc");
        assertThat(event.chain()).isEqualTo(Chain.ETHEREUM);
        assertThat(event.language()).isEqualTo(Language.RU);
        assertThat(event.requestId()).isNotNull();
        assertThat(event.requestedAt()).isNotNull();
    }

    @Test
    void toAddRequestedGeneratesDistinctRequestIdsAcrossCalls() {
        WatchlistAddRequested first = mapper.toAddRequested(UUID.randomUUID(), "0xabc", Chain.ETHEREUM, Language.EN);
        WatchlistAddRequested second = mapper.toAddRequested(UUID.randomUUID(), "0xabc", Chain.ETHEREUM, Language.EN);

        assertThat(first.requestId()).isNotEqualTo(second.requestId());
    }

    @Test
    void toRemoveRequestedCarriesThroughGivenFieldsWithFreshIdAndTimestamp() {
        UUID userId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        WatchlistRemoveRequested event = mapper.toRemoveRequested(userId, entryId);

        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.entryId()).isEqualTo(entryId);
        assertThat(event.requestId()).isNotNull();
        assertThat(event.requestedAt()).isNotNull();
    }

    @Test
    void toViewMapsAllFieldsFromRow() {
        WatchlistEntryRow row = new WatchlistEntryRow(UUID.randomUUID(), "0xabc", Chain.ETHEREUM,
                RiskLevel.LOW, 10, UUID.randomUUID(), Instant.now(), Instant.now());

        WatchlistEntryView view = mapper.toView(row);

        assertThat(view).isEqualTo(new WatchlistEntryView(row.id(), row.address(), row.chain(), row.lastRiskLevel(),
                row.lastScore(), row.lastScanId(), row.lastCheckedAt(), row.createdAt()));
    }
}
