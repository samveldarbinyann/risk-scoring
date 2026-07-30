package com.riskscoring.monitor.service.impl;

import com.riskscoring.common.event.AlertTriggered;
import com.riskscoring.common.event.ScanCompleted;
import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.monitor.config.MonitorProperties;
import com.riskscoring.monitor.entity.Alert;
import com.riskscoring.monitor.entity.WatchlistEntry;
import com.riskscoring.monitor.kafka.MonitorEventPublisher;
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
            watchlistEntryRepository.save(entry);

            eventPublisher.publishScanRequested(new ScanRequested(
                    scanId, entry.getAddress(), entry.getChainId(), now, ScanSource.MONITOR, entry.getLanguage()));
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
            watchlistEntryRepository.save(entry);
        });
    }

    private void raiseAlert(WatchlistEntry entry, RiskLevel newLevel, int newScore, UUID scanId) {
        Instant now = Instant.now();

        Alert alert = Alert.builder()
                .id(UUID.randomUUID())
                .watchlistEntryId(entry.getId())
                .userId(entry.getUserId())
                .address(entry.getAddress())
                .chainId(entry.getChainId())
                .previousRiskLevel(entry.getLastRiskLevel())
                .previousScore(entry.getLastScore())
                .newRiskLevel(newLevel)
                .newScore(newScore)
                .scanId(scanId)
                .triggeredAt(now)
                .createdAt(now)
                .build();
        alertRepository.save(alert);

        eventPublisher.publishAlertTriggered(new AlertTriggered(
                alert.getId(), entry.getId(), entry.getUserId(), entry.getAddress(), entry.getChainId(),
                entry.getLastRiskLevel(), entry.getLastScore(), newLevel, newScore, scanId, now));

        log.info("Alert triggered watchlistEntryId={} {} -> {}", entry.getId(), entry.getLastRiskLevel(), newLevel);
    }
}
