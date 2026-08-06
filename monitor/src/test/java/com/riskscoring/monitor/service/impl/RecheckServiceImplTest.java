package com.riskscoring.monitor.service.impl;

import com.riskscoring.common.event.AlertTriggered;
import com.riskscoring.common.event.ScanCompleted;
import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.Verdict;
import com.riskscoring.monitor.config.MonitorProperties;
import com.riskscoring.monitor.entity.Alert;
import com.riskscoring.monitor.entity.WatchlistEntry;
import com.riskscoring.monitor.kafka.MonitorEventPublisher;
import com.riskscoring.monitor.mapper.AlertMapper;
import com.riskscoring.monitor.mapper.WatchlistMapper;
import com.riskscoring.monitor.repository.AlertRepository;
import com.riskscoring.monitor.repository.WatchlistEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecheckServiceImplTest {

    private static final MonitorProperties PROPERTIES = new MonitorProperties(Duration.ofHours(6), 50, Duration.ofHours(1));
    private static final Chain CHAIN = Chain.ETHEREUM;
    private static final String ADDRESS = "0xabc";

    @Mock
    private WatchlistEntryRepository watchlistEntryRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private MonitorEventPublisher eventPublisher;
    @Mock
    private WatchlistMapper watchlistMapper;
    @Mock
    private AlertMapper alertMapper;

    private RecheckServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RecheckServiceImpl(
                watchlistEntryRepository, alertRepository, eventPublisher, watchlistMapper, alertMapper, PROPERTIES);
    }

    @Test
    void triggerDueRechecksDoesNothingWhenNoEntriesDue() {
        when(watchlistEntryRepository.findDueForRecheck(any(), any(), any())).thenReturn(List.of());

        service.triggerDueRechecks();

        verifyNoInteractions(eventPublisher);
        verify(watchlistEntryRepository, never()).saveAll(any());
    }

    @Test
    void triggerDueRechecksPassesThresholdsDerivedFromPropertiesWithCorrectGap() {
        when(watchlistEntryRepository.findDueForRecheck(any(), any(), any())).thenReturn(List.of());

        service.triggerDueRechecks();

        ArgumentCaptor<Instant> dueCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> staleCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(watchlistEntryRepository).findDueForRecheck(dueCaptor.capture(), staleCaptor.capture(), any());

        assertThat(Duration.between(dueCaptor.getValue(), staleCaptor.getValue()))
                .isEqualTo(PROPERTIES.recheckInterval().minus(PROPERTIES.stalePendingTimeout()));
    }

    @Test
    void triggerDueRechecksPassesFirstPageWithConfiguredBatchSize() {
        when(watchlistEntryRepository.findDueForRecheck(any(), any(), any())).thenReturn(List.of());

        service.triggerDueRechecks();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(watchlistEntryRepository).findDueForRecheck(any(), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void triggerDueRechecksSetsPendingScanIdAndPendingRequestedAtOnEachDueEntry() {
        WatchlistEntry entry = watchlistEntry(null);
        when(watchlistEntryRepository.findDueForRecheck(any(), any(), any())).thenReturn(List.of(entry));
        stubToScanRequestedEcho();

        service.triggerDueRechecks();

        assertThat(entry.getPendingScanId()).isNotNull();
        assertThat(entry.getPendingRequestedAt()).isNotNull();
        assertThat(entry.getUpdatedAt()).isNotNull();
    }

    @Test
    void triggerDueRechecksPublishesScanRequestedWithSameScanIdAsPendingScanId() {
        WatchlistEntry entry = watchlistEntry(null);
        when(watchlistEntryRepository.findDueForRecheck(any(), any(), any())).thenReturn(List.of(entry));
        stubToScanRequestedEcho();

        service.triggerDueRechecks();

        ArgumentCaptor<ScanRequested> captor = ArgumentCaptor.forClass(ScanRequested.class);
        verify(eventPublisher).publishScanRequested(captor.capture());
        assertThat(captor.getValue().scanId()).isEqualTo(entry.getPendingScanId());
    }

    @Test
    void triggerDueRechecksSavesAllDueEntriesAfterProcessing() {
        WatchlistEntry entry = watchlistEntry(null);
        when(watchlistEntryRepository.findDueForRecheck(any(), any(), any())).thenReturn(List.of(entry));
        stubToScanRequestedEcho();

        service.triggerDueRechecks();

        verify(watchlistEntryRepository).saveAll(List.of(entry));
    }

    @Test
    void handleScanCompletedAppliesResultToPendingEntryAndSkipsActiveEntryLookup() {
        WatchlistEntry entry = watchlistEntry(null);
        ScanCompleted event = scanCompleted(RiskLevel.LOW, 15);
        when(watchlistEntryRepository.findByPendingScanId(event.scanId())).thenReturn(Optional.of(entry));

        service.handleScanCompleted(event);

        verify(watchlistEntryRepository, never()).findByChainAndAddressAndActiveTrue(any(), any());
        verify(watchlistEntryRepository).save(entry);
        assertThat(entry.getLastRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(entry.getLastScore()).isEqualTo(15);
        assertThat(entry.getLastScanId()).isEqualTo(event.scanId());
        assertThat(entry.getLastCheckedAt()).isNotNull();
        assertThat(entry.getPendingScanId()).isNull();
        assertThat(entry.getPendingRequestedAt()).isNull();
    }

    @Test
    void handleScanCompletedDoesNotRaiseAlertWhenLastRiskLevelWasNull() {
        WatchlistEntry entry = watchlistEntry(null);
        ScanCompleted event = scanCompleted(RiskLevel.HIGH, 70);
        when(watchlistEntryRepository.findByPendingScanId(event.scanId())).thenReturn(Optional.of(entry));

        service.handleScanCompleted(event);

        verifyNoInteractions(alertRepository, alertMapper);
        verify(eventPublisher, never()).publishAlertTriggered(any());
    }

    @Test
    void handleScanCompletedDoesNotRaiseAlertWhenRiskLevelUnchanged() {
        WatchlistEntry entry = watchlistEntry(RiskLevel.HIGH);
        ScanCompleted event = scanCompleted(RiskLevel.HIGH, 75);
        when(watchlistEntryRepository.findByPendingScanId(event.scanId())).thenReturn(Optional.of(entry));

        service.handleScanCompleted(event);

        verifyNoInteractions(alertRepository, alertMapper);
        verify(eventPublisher, never()).publishAlertTriggered(any());
    }

    @Test
    void handleScanCompletedRaisesAlertWhenRiskLevelChanges() {
        WatchlistEntry entry = watchlistEntry(RiskLevel.LOW);
        ScanCompleted event = scanCompleted(RiskLevel.HIGH, 70);
        when(watchlistEntryRepository.findByPendingScanId(event.scanId())).thenReturn(Optional.of(entry));
        Alert alert = alert();
        when(alertMapper.toEntity(eq(entry), eq(RiskLevel.HIGH), eq(70), eq(event.scanId()), any()))
                .thenReturn(alert);
        when(alertMapper.toEvent(alert)).thenReturn(alertTriggered(alert));

        service.handleScanCompleted(event);

        InOrder order = inOrder(alertRepository, eventPublisher);
        order.verify(alertRepository).save(alert);
        order.verify(eventPublisher).publishAlertTriggered(alertTriggered(alert));
    }

    @Test
    void handleScanCompletedFallsBackToActiveEntriesWhenNoPendingEntryFound() {
        WatchlistEntry entry1 = watchlistEntry(null);
        WatchlistEntry entry2 = watchlistEntry(null);
        ScanCompleted event = scanCompleted(RiskLevel.LOW, 5);
        when(watchlistEntryRepository.findByPendingScanId(event.scanId())).thenReturn(Optional.empty());
        when(watchlistEntryRepository.findByChainAndAddressAndActiveTrue(CHAIN, ADDRESS))
                .thenReturn(List.of(entry1, entry2));

        service.handleScanCompleted(event);

        verify(watchlistEntryRepository, times(2)).save(any());
        assertThat(entry1.getLastRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(entry2.getLastRiskLevel()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void handleScanCompletedDoesNothingWhenNeitherPendingNorActiveEntriesFound() {
        ScanCompleted event = scanCompleted(RiskLevel.LOW, 5);
        when(watchlistEntryRepository.findByPendingScanId(event.scanId())).thenReturn(Optional.empty());
        when(watchlistEntryRepository.findByChainAndAddressAndActiveTrue(CHAIN, ADDRESS)).thenReturn(List.of());

        service.handleScanCompleted(event);

        verify(watchlistEntryRepository, never()).save(any());
        verifyNoInteractions(alertRepository, eventPublisher);
    }

    private void stubToScanRequestedEcho() {
        lenient().when(watchlistMapper.toScanRequested(any(), any(), any())).thenAnswer(invocation -> {
            WatchlistEntry entry = invocation.getArgument(0);
            UUID scanId = invocation.getArgument(1);
            Instant requestedAt = invocation.getArgument(2);
            return new ScanRequested(scanId, ScanTarget.ADDRESS, entry.getAddress(), entry.getChain(),
                    requestedAt, ScanSource.MONITOR, entry.getLanguage(), entry.getUserId());
        });
    }

    private static WatchlistEntry watchlistEntry(RiskLevel lastRiskLevel) {
        return WatchlistEntry.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .address(ADDRESS)
                .chain(CHAIN)
                .active(true)
                .lastRiskLevel(lastRiskLevel)
                .lastScore(lastRiskLevel == null ? null : 10)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private static ScanCompleted scanCompleted(RiskLevel riskLevel, int score) {
        Verdict verdict = new Verdict(riskLevel, score, "explanation", List.of(), List.of());
        return new ScanCompleted(UUID.randomUUID(), ScanTarget.ADDRESS, ADDRESS, CHAIN, verdict, "deepseek-chat", Instant.now());
    }

    private static Alert alert() {
        return Alert.builder()
                .id(UUID.randomUUID())
                .watchlistEntryId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .address(ADDRESS)
                .chain(CHAIN)
                .previousRiskLevel(RiskLevel.LOW)
                .previousScore(10)
                .newRiskLevel(RiskLevel.HIGH)
                .newScore(70)
                .scanId(UUID.randomUUID())
                .triggeredAt(Instant.now())
                .build();
    }

    private static AlertTriggered alertTriggered(Alert alert) {
        return new AlertTriggered(
                alert.getId(), alert.getWatchlistEntryId(), alert.getUserId(), alert.getAddress(), alert.getChain(),
                alert.getPreviousRiskLevel(), alert.getPreviousScore(), alert.getNewRiskLevel(), alert.getNewScore(),
                alert.getScanId(), alert.getTriggeredAt());
    }
}
