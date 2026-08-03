package com.riskscoring.monitor.service.impl;

import com.riskscoring.common.event.ScanCompleted;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.monitor.config.MonitorProperties;
import com.riskscoring.monitor.entity.Alert;
import com.riskscoring.monitor.entity.WatchlistEntry;
import com.riskscoring.monitor.kafka.MonitorEventPublisher;
import com.riskscoring.monitor.mapper.AlertMapper;
import com.riskscoring.monitor.mapper.WatchlistMapper;
import com.riskscoring.monitor.repository.AlertRepository;
import com.riskscoring.monitor.repository.WatchlistEntryRepository;
import com.riskscoring.monitor.service.RecheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecheckServiceImpl implements RecheckService {

    private final WatchlistEntryRepository watchlistEntryRepository;
    private final AlertRepository alertRepository;
    private final MonitorEventPublisher eventPublisher;
    private final WatchlistMapper watchlistMapper;
    private final AlertMapper alertMapper;
    private final MonitorProperties properties;

    @Override
    @Scheduled(fixedDelayString = "${monitor.scheduler-fixed-delay}")
    @Transactional
    public void triggerDueRechecks() {
        Instant now = Instant.now();
        Instant dueThreshold = now.minus(properties.recheckInterval());
        Instant staleThreshold = now.minus(properties.stalePendingTimeout());

        List<WatchlistEntry> due = watchlistEntryRepository.findDueForRecheck(
                dueThreshold, staleThreshold, PageRequest.of(0, properties.batchSize()));

        for (WatchlistEntry entry : due) {
            UUID scanId = UUID.randomUUID();
            entry.setPendingScanId(scanId);
            entry.setPendingRequestedAt(now);
            entry.setUpdatedAt(now);

            eventPublisher.publishScanRequested(watchlistMapper.toScanRequested(entry, scanId, now));
        }

        if (!due.isEmpty()) {
            watchlistEntryRepository.saveAll(due);
            log.info("Triggered {} watchlist recheck(s)", due.size());
        }
    }

    @Override
    @Transactional
    public void handleScanCompleted(ScanCompleted event) {
        var pending = watchlistEntryRepository.findByPendingScanId(event.scanId());
        if (pending.isPresent()) {
            applyScanResult(pending.get(), event);
            return;
        }

        var entries = watchlistEntryRepository.findByChainIdAndAddressAndActiveTrue(
                event.chainId(), event.target());
        for (WatchlistEntry entry : entries) {
            applyScanResult(entry, event);
        }
        if (!entries.isEmpty()) {
            log.info("Updated {} watchlist entries from non-monitor scan scanId={}",
                    entries.size(), event.scanId());
        }
    }

    private void applyScanResult(WatchlistEntry entry, ScanCompleted event) {
        RiskLevel newLevel = event.verdict().riskLevel();
        int newScore = event.verdict().score();

        if (entry.getLastRiskLevel() != null && entry.getLastRiskLevel() != newLevel) {
            raiseAlert(entry, newLevel, newScore, event.scanId());
        }

        Instant now = Instant.now();
        entry.setLastRiskLevel(newLevel);
        entry.setLastScore(newScore);
        entry.setLastScanId(event.scanId());
        entry.setLastCheckedAt(now);
        entry.setPendingScanId(null);
        entry.setPendingRequestedAt(null);
        entry.setUpdatedAt(now);
        watchlistEntryRepository.save(entry);
    }

    private void raiseAlert(WatchlistEntry entry, RiskLevel newLevel, int newScore, UUID scanId) {
        Alert alert = alertMapper.toEntity(entry, newLevel, newScore, scanId, Instant.now());
        alertRepository.save(alert);

        eventPublisher.publishAlertTriggered(alertMapper.toEvent(alert));

        log.info("Alert triggered watchlistEntryId={} {} -> {}", entry.getId(), entry.getLastRiskLevel(), newLevel);
    }
}
