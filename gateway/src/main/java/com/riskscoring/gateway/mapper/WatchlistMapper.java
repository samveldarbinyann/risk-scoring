package com.riskscoring.gateway.mapper;

import com.riskscoring.common.event.WatchlistAddRequested;
import com.riskscoring.common.event.WatchlistRemoveRequested;
import com.riskscoring.common.model.Language;
import com.riskscoring.gateway.dto.WatchlistEntryView;
import com.riskscoring.gateway.repository.WatchlistEntryRow;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class WatchlistMapper {

    public WatchlistAddRequested toAddRequested(UUID userId, String address, int chainId, Language language) {
        return new WatchlistAddRequested(UUID.randomUUID(), userId, address, chainId, language, Instant.now());
    }

    public WatchlistRemoveRequested toRemoveRequested(UUID userId, UUID entryId) {
        return new WatchlistRemoveRequested(UUID.randomUUID(), userId, entryId, Instant.now());
    }

    public WatchlistEntryView toView(WatchlistEntryRow row) {
        return new WatchlistEntryView(
                row.id(),
                row.address(),
                row.chainId(),
                row.lastRiskLevel(),
                row.lastScore(),
                row.lastScanId(),
                row.lastCheckedAt(),
                row.createdAt()
        );
    }

}
