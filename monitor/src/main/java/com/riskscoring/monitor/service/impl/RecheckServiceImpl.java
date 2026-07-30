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

            eventPublisher.publishScanRequested(watchlistMapper.toScanRequested(entry, scanId, now));
        }

        if (!due.isEmpty()) {
            log.info("Triggered {} watchlist recheck(s)", due.size());
        }
    }

    @Override
    @Transactional
    public void handleScanCompleted(ScanCompleted event) {
        watchlistEntryRepository.findByPendingScanId(event.scanId()).ifPresent(entry -> {
            RiskLevel newLevel = event.verdict().riskLevel();
            int newScore = event.verdict().score();

            if (entry.getLastRiskLevel() != null && entry.getLastRiskLevel() != newLevel) {
                raiseAlert(entry, newLevel, newScore, event.scanId());
            }

            entry.setLastRiskLevel(newLevel);
            entry.setLastScore(newScore);
            entry.setLastScanId(event.scanId());
            entry.setLastCheckedAt(Instant.now());
            entry.setPendingScanId(null);
            entry.setPendingRequestedAt(null);
        });
    }

    private void raiseAlert(WatchlistEntry entry, RiskLevel newLevel, int newScore, UUID scanId) {
        Alert alert = alertMapper.toEntity(entry, newLevel, newScore, scanId, Instant.now());
        alertRepository.save(alert);

        eventPublisher.publishAlertTriggered(alertMapper.toEvent(alert));

        log.info("Alert triggered watchlistEntryId={} {} -> {}", entry.getId(), entry.getLastRiskLevel(), newLevel);
    }
}
