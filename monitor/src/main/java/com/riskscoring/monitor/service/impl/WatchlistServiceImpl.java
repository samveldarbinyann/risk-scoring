package com.riskscoring.monitor.service.impl;

import com.riskscoring.common.event.WatchlistAddRequested;
import com.riskscoring.common.event.WatchlistRemoveRequested;
import com.riskscoring.monitor.entity.WatchlistEntry;
import com.riskscoring.monitor.repository.WatchlistEntryRepository;
import com.riskscoring.monitor.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchlistServiceImpl implements WatchlistService {

    private final WatchlistEntryRepository watchlistEntryRepository;

    @Override
    @Transactional
    public void addEntry(WatchlistAddRequested event) {
        Instant now = Instant.now();

        WatchlistEntry entry = watchlistEntryRepository
                .findByUserIdAndChainIdAndAddress(event.userId(), event.chainId(), event.address())
                .map(existing -> reactivate(existing, event, now))
                .orElseGet(() -> newEntry(event, now));

        watchlistEntryRepository.save(entry);
        log.info("Watchlist entry upserted id={} userId={} address={} chainId={}",
                entry.getId(), event.userId(), event.address(), event.chainId());
    }

    @Override
    @Transactional
    public void removeEntry(WatchlistRemoveRequested event) {
        watchlistEntryRepository.findByIdAndUserId(event.entryId(), event.userId())
                .ifPresentOrElse(entry -> {
                    entry.setActive(false);
                    entry.setUpdatedAt(Instant.now());
                    watchlistEntryRepository.save(entry);
                    log.info("Watchlist entry deactivated id={}", entry.getId());
                }, () -> log.warn("Watchlist entry not found or not owned by user, ignoring: entryId={} userId={}",
                        event.entryId(), event.userId()));
    }

    private WatchlistEntry reactivate(WatchlistEntry entry, WatchlistAddRequested event, Instant now) {
        entry.setActive(true);
        entry.setLanguage(event.language());
        entry.setUpdatedAt(now);
        return entry;
    }

    private WatchlistEntry newEntry(WatchlistAddRequested event, Instant now) {
        return WatchlistEntry.builder()
                .id(UUID.randomUUID())
                .userId(event.userId())
                .address(event.address())
                .chainId(event.chainId())
                .language(event.language())
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
