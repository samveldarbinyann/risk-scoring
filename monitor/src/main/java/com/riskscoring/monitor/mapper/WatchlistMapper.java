package com.riskscoring.monitor.mapper;

import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.event.WatchlistAddRequested;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.monitor.entity.WatchlistEntry;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class WatchlistMapper {

    public WatchlistEntry toEntity(WatchlistAddRequested event, Instant now) {
        return WatchlistEntry.builder()
                .id(UUID.randomUUID())
                .userId(event.userId())
                .address(event.address())
                .chain(event.chain())
                .language(event.language())
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public ScanRequested toScanRequested(WatchlistEntry entry, UUID scanId, Instant requestedAt) {
        return new ScanRequested(
                scanId, ScanTarget.ADDRESS, entry.getAddress(), entry.getChain(),
                requestedAt, ScanSource.MONITOR, entry.getLanguage(), entry.getUserId());
    }
}