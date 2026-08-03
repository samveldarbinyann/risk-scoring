package com.riskscoring.monitor.service.impl;

import com.riskscoring.common.event.WatchlistAddRequested;
import com.riskscoring.common.event.WatchlistRemoveRequested;
import com.riskscoring.common.model.Chain;
import com.riskscoring.monitor.entity.WatchlistEntry;
import com.riskscoring.monitor.mapper.WatchlistMapper;
import com.riskscoring.monitor.repository.WatchlistEntryRepository;
import com.riskscoring.monitor.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchlistServiceImpl implements WatchlistService {

    private final WatchlistEntryRepository watchlistEntryRepository;
    private final WatchlistMapper watchlistMapper;

    @Override
    @Transactional
    public void addEntry(WatchlistAddRequested event) {
        Instant now = Instant.now();

        WatchlistEntry entry = watchlistEntryRepository
                .findByUserIdAndChainAndAddress(event.userId(), event.chain(), event.address())
                .map(existing -> reactivate(existing, event, now))
                .orElseGet(() -> watchlistEntryRepository.save(watchlistMapper.toEntity(event, now)));

        log.info("Watchlist entry upserted id={} userId={} address={} chain={}",
                entry.getId(), event.userId(), event.address(), event.chain());
    }

    @Override
    @Transactional
    public void removeEntry(WatchlistRemoveRequested event) {
        watchlistEntryRepository.findByIdAndUserId(event.entryId(), event.userId())
                .ifPresentOrElse(entry -> {
                    entry.setActive(false);
                    entry.setUpdatedAt(Instant.now());
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
}
