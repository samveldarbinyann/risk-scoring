package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.model.AddressEvidence;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.Heuristics;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.gateway.dto.RecentScanGroupView;
import com.riskscoring.gateway.dto.ScanCreateRequest;
import com.riskscoring.gateway.dto.ScanGroupAcceptedResponse;
import com.riskscoring.gateway.dto.ScanGroupReportView;
import com.riskscoring.gateway.dto.ScanGroupView;
import com.riskscoring.gateway.dto.ScanHistoryPageView;
import com.riskscoring.gateway.dto.ScanReportView;
import com.riskscoring.gateway.dto.ScanView;
import com.riskscoring.gateway.entity.Scan;
import com.riskscoring.gateway.entity.ScanGroup;
import com.riskscoring.gateway.exception.QuotaExceededException;
import com.riskscoring.gateway.exception.ScanGroupNotFoundException;
import com.riskscoring.gateway.exception.ScanGroupReportNotReadyException;
import com.riskscoring.gateway.exception.ScanNotFoundException;
import com.riskscoring.gateway.exception.ScanReportNotReadyException;
import com.riskscoring.gateway.exception.SingleChainRequiredException;
import com.riskscoring.gateway.exception.TargetChainMismatchException;
import com.riskscoring.gateway.kafka.ScanEventPublisher;
import com.riskscoring.gateway.mapper.ScanMapper;
import com.riskscoring.gateway.repository.ScanGroupRepository;
import com.riskscoring.gateway.repository.ScanReportRepository;
import com.riskscoring.gateway.repository.ScanReportRow;
import com.riskscoring.gateway.repository.ScanRepository;
import com.riskscoring.gateway.repository.ScanRiskSummary;
import com.riskscoring.gateway.service.BillingService;
import com.riskscoring.gateway.service.ChainService;
import com.riskscoring.gateway.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("8dc2f3fd-f443-4481-842e-5d70a6cb4b88");
    private static final String CLIENT_IP = "203.0.113.10";

    @Mock
    private ScanGroupRepository scanGroupRepository;

    @Mock
    private ScanRepository scanRepository;

    @Mock
    private ScanReportRepository scanReportRepository;

    @Mock
    private ScanEventPublisher scanEventPublisher;

    @Mock
    private BillingService billingService;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private ChainService chainService;

    @Captor
    private ArgumentCaptor<ScanGroup> scanGroupCaptor;

    @Captor
    private ArgumentCaptor<Scan> scanCaptor;

    private ScanServiceImpl scanService;

    @BeforeEach
    void setUp() {
        scanService = new ScanServiceImpl(scanGroupRepository, scanRepository, scanReportRepository,
                new ScanMapper(), scanEventPublisher, billingService, rateLimitService, chainService);
    }

    @Test
    void getScanHistoryFiltersBySourceAndPaginates() {
        ScanGroup group = ScanGroup.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .targetType(ScanTarget.ADDRESS)
                .target("0xabc")
                .requestedAt(Instant.now())
                .build();
        Scan scan = Scan.builder()
                .id(UUID.randomUUID())
                .groupId(group.getId())
                .targetType(ScanTarget.ADDRESS)
                .target("0xabc")
                .chain(Chain.ETHEREUM)
                .status(ScanStage.COMPLETED)
                .source(ScanSource.API)
                .requestedAt(Instant.now())
                .build();
        Pageable pageable = PageRequest.of(0, 20);
        when(scanGroupRepository.findHistory(eq(USER_ID), eq(ScanSource.API), any()))
                .thenReturn(new PageImpl<>(List.of(group), pageable, 1));
        when(scanRepository.findByGroupIdIn(List.of(group.getId()))).thenReturn(List.of(scan));
        when(scanReportRepository.findRiskSummaries(any())).thenReturn(List.of());

        ScanHistoryPageView result = scanService.getScanHistory(USER_ID, ScanSource.API, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().source()).isEqualTo(ScanSource.API);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void getScanHistoryClampsPageSizeWithinBounds() {
        when(scanGroupRepository.findHistory(eq(USER_ID), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        scanService.getScanHistory(USER_ID, null, 0, 500);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(scanGroupRepository).findHistory(eq(USER_ID), isNull(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void ingestMonitorScanCreatesScanGroupAndScanOnFirstEvent() {
        UUID scanId = UUID.randomUUID();
        ScanRequested event = new ScanRequested(scanId, ScanTarget.ADDRESS, "0xabc", Chain.ETHEREUM,
                Instant.now(), ScanSource.MONITOR, Language.EN, USER_ID);
        when(scanRepository.existsById(scanId)).thenReturn(false);

        scanService.ingestMonitorScan(event);

        verify(scanGroupRepository).save(scanGroupCaptor.capture());
        ScanGroup group = scanGroupCaptor.getValue();
        assertThat(group.getUserId()).isEqualTo(USER_ID);
        assertThat(group.getTarget()).isEqualTo("0xabc");

        verify(scanRepository).save(scanCaptor.capture());
        Scan scan = scanCaptor.getValue();
        assertThat(scan.getId()).isEqualTo(scanId);
        assertThat(scan.getGroupId()).isEqualTo(group.getId());
        assertThat(scan.getSource()).isEqualTo(ScanSource.MONITOR);
        assertThat(scan.getStatus()).isEqualTo(ScanStage.PENDING);
    }

    @Test
    void ingestMonitorScanIsIdempotentOnDuplicateEvent() {
        UUID scanId = UUID.randomUUID();
        ScanRequested event = new ScanRequested(scanId, ScanTarget.ADDRESS, "0xabc", Chain.ETHEREUM,
                Instant.now(), ScanSource.MONITOR, Language.EN, USER_ID);
        when(scanRepository.existsById(scanId)).thenReturn(true);

        scanService.ingestMonitorScan(event);

        verify(scanGroupRepository, never()).save(any());
        verify(scanRepository, never()).save(any());
    }

    @Test
    void requestScanChecksRateLimitAndFansOutToEveryScannableChainWhenNoneRequestedExplicitly() {
        String address = "0x" + "a".repeat(40);

        ScanGroupAcceptedResponse response = scanService.requestScan(CLIENT_IP, USER_ID, new ScanCreateRequest(address, null));

        verify(rateLimitService).checkPublicScan(CLIENT_IP);
        assertThat(response.chains()).hasSize(Chain.of(Chain.ETHEREUM.family()).size());
        verify(scanEventPublisher, times(response.chains().size())).publishScanRequested(any());
    }

    @Test
    void requestScanThrowsSingleChainRequiredExceptionWhenMultipleTransactionChainsRequestedExplicitly() {
        String txHash = "0x" + "a".repeat(64);
        when(chainService.requireScannable("ETHEREUM")).thenReturn(Chain.ETHEREUM);
        when(chainService.requireScannable("OPTIMISM")).thenReturn(Chain.OPTIMISM);

        assertThatThrownBy(() -> scanService.requestScan(CLIENT_IP, USER_ID,
                new ScanCreateRequest(txHash, List.of("ETHEREUM", "OPTIMISM"))))
                .isInstanceOf(SingleChainRequiredException.class);

        verifyNoInteractions(scanEventPublisher);
    }

    @Test
    void requestScanAllowsASingleExplicitTransactionChain() {
        String txHash = "0x" + "a".repeat(64);
        when(chainService.requireScannable("ETHEREUM")).thenReturn(Chain.ETHEREUM);

        ScanGroupAcceptedResponse response = scanService.requestScan(CLIENT_IP, USER_ID,
                new ScanCreateRequest(txHash, List.of("ETHEREUM")));

        assertThat(response.chains()).containsExactly(Chain.ETHEREUM);
    }

    @Test
    void requestScanDedupesDuplicateExplicitChains() {
        String txHash = "0x" + "a".repeat(64);
        when(chainService.requireScannable("ETHEREUM")).thenReturn(Chain.ETHEREUM);

        ScanGroupAcceptedResponse response = scanService.requestScan(CLIENT_IP, USER_ID,
                new ScanCreateRequest(txHash, List.of("ETHEREUM", "ETHEREUM")));

        assertThat(response.chains()).containsExactly(Chain.ETHEREUM);
    }

    @Test
    void requestScanThrowsTargetChainMismatchExceptionWhenExplicitChainDoesNotMatchTargetFormat() {
        String evmAddress = "0x" + "a".repeat(40);
        when(chainService.requireScannable("BITCOIN")).thenReturn(Chain.BITCOIN);

        assertThatThrownBy(() -> scanService.requestScan(CLIENT_IP, USER_ID,
                new ScanCreateRequest(evmAddress, List.of("BITCOIN"))))
                .isInstanceOf(TargetChainMismatchException.class);
    }

    @Test
    void requestApiScanChargesQuotaForNumberOfMatchesBeforeCreatingScanGroup() {
        String address = "0x" + "a".repeat(40);
        when(chainService.requireScannable("ETHEREUM")).thenReturn(Chain.ETHEREUM);

        scanService.requestApiScan(USER_ID, new ScanCreateRequest(address, List.of("ETHEREUM")));

        var order = inOrder(billingService, scanGroupRepository);
        order.verify(billingService).chargeQuota(USER_ID, 1);
        order.verify(scanGroupRepository).save(any());
    }

    @Test
    void requestApiScanPropagatesQuotaExceededExceptionWithoutCreatingScanGroup() {
        String address = "0x" + "a".repeat(40);
        when(chainService.requireScannable("ETHEREUM")).thenReturn(Chain.ETHEREUM);
        doThrow(new QuotaExceededException(10, 10, 1)).when(billingService).chargeQuota(USER_ID, 1);

        assertThatThrownBy(() -> scanService.requestApiScan(USER_ID, new ScanCreateRequest(address, List.of("ETHEREUM"))))
                .isInstanceOf(QuotaExceededException.class);

        verifyNoInteractions(scanGroupRepository, scanEventPublisher);
    }

    @Test
    void getRecentScansReturnsEmptyListWhenUserHasNoGroups() {
        when(scanGroupRepository.findTop5ByUserIdOrderByRequestedAtDesc(USER_ID)).thenReturn(List.of());

        List<RecentScanGroupView> views = scanService.getRecentScans(USER_ID);

        assertThat(views).isEmpty();
        verifyNoInteractions(scanRepository);
    }

    @Test
    void getRecentScansReturnsMappedViewsForRecentGroups() {
        ScanGroup group = scanGroup();
        Scan scan = completedScan(group.getId());
        when(scanGroupRepository.findTop5ByUserIdOrderByRequestedAtDesc(USER_ID)).thenReturn(List.of(group));
        when(scanRepository.findByGroupIdIn(List.of(group.getId()))).thenReturn(List.of(scan));
        when(scanReportRepository.findRiskSummaries(List.of(scan.getId())))
                .thenReturn(List.of(new ScanRiskSummary(scan.getId(), RiskLevel.LOW, 10)));

        List<RecentScanGroupView> views = scanService.getRecentScans(USER_ID);

        assertThat(views).hasSize(1);
        assertThat(views.getFirst().worstRiskLevel()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void getScanGroupThrowsScanGroupNotFoundExceptionWhenNoScansExist() {
        UUID groupId = UUID.randomUUID();
        when(scanRepository.findByGroupId(groupId)).thenReturn(List.of());

        assertThatThrownBy(() -> scanService.getScanGroup(groupId)).isInstanceOf(ScanGroupNotFoundException.class);
    }

    @Test
    void getScanGroupReturnsMappedViewWhenScansExist() {
        ScanGroup group = scanGroup();
        Scan scan = completedScan(group.getId());
        when(scanRepository.findByGroupId(group.getId())).thenReturn(List.of(scan));

        ScanGroupView view = scanService.getScanGroup(group.getId());

        assertThat(view.groupId()).isEqualTo(group.getId());
        assertThat(view.completed()).isTrue();
    }

    @Test
    void getScanGroupReportThrowsScanGroupReportNotReadyExceptionWhenGroupIsNotYetCompleted() {
        ScanGroup group = scanGroup();
        Scan pending = Scan.builder()
                .id(UUID.randomUUID())
                .groupId(group.getId())
                .targetType(ScanTarget.ADDRESS)
                .target("0xabc")
                .chain(Chain.ETHEREUM)
                .status(ScanStage.PENDING)
                .source(ScanSource.USER)
                .requestedAt(Instant.now())
                .build();
        when(scanRepository.findByGroupId(group.getId())).thenReturn(List.of(pending));

        assertThatThrownBy(() -> scanService.getScanGroupReport(group.getId()))
                .isInstanceOf(ScanGroupReportNotReadyException.class);
    }

    @Test
    void getScanGroupReportSilentlyDropsChainsWithoutAReportRowYet() {
        ScanGroup group = scanGroup();
        Scan scan = completedScan(group.getId());
        when(scanRepository.findByGroupId(group.getId())).thenReturn(List.of(scan));
        when(scanReportRepository.findByScanId(scan.getId())).thenReturn(Optional.empty());

        ScanGroupReportView report = scanService.getScanGroupReport(group.getId());

        assertThat(report.reports()).isEmpty();
    }

    @Test
    void getScanGroupReportReturnsReportsForCompletedChainsSortedByChain() {
        ScanGroup group = scanGroup();
        Scan ethereum = completedScan(group.getId(), Chain.ETHEREUM);
        Scan bitcoin = completedScan(group.getId(), Chain.BITCOIN);
        when(scanRepository.findByGroupId(group.getId())).thenReturn(List.of(ethereum, bitcoin));
        when(scanReportRepository.findByScanId(ethereum.getId())).thenReturn(Optional.of(reportRow(ethereum)));
        when(scanReportRepository.findByScanId(bitcoin.getId())).thenReturn(Optional.of(reportRow(bitcoin)));

        ScanGroupReportView report = scanService.getScanGroupReport(group.getId());

        assertThat(report.reports()).hasSize(2);
        assertThat(report.reports()).extracting(ScanReportView::chain).containsExactly(Chain.ETHEREUM, Chain.BITCOIN);
    }

    @Test
    void getScanThrowsScanNotFoundExceptionWhenScanIsMissing() {
        UUID scanId = UUID.randomUUID();
        when(scanRepository.findById(scanId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scanService.getScan(scanId)).isInstanceOf(ScanNotFoundException.class);
    }

    @Test
    void getScanReturnsMappedViewWhenFound() {
        Scan scan = completedScan(UUID.randomUUID());
        when(scanRepository.findById(scan.getId())).thenReturn(Optional.of(scan));

        ScanView view = scanService.getScan(scan.getId());

        assertThat(view.scanId()).isEqualTo(scan.getId());
    }

    @Test
    void getScanReportThrowsScanNotFoundExceptionWhenScanIsMissing() {
        UUID scanId = UUID.randomUUID();
        when(scanRepository.findById(scanId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scanService.getScanReport(scanId)).isInstanceOf(ScanNotFoundException.class);
    }

    @Test
    void getScanReportThrowsScanReportNotReadyExceptionWhenScanNotYetCompleted() {
        Scan pending = Scan.builder()
                .id(UUID.randomUUID())
                .groupId(UUID.randomUUID())
                .targetType(ScanTarget.ADDRESS)
                .target("0xabc")
                .chain(Chain.ETHEREUM)
                .status(ScanStage.ANALYZING)
                .source(ScanSource.USER)
                .requestedAt(Instant.now())
                .build();
        when(scanRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> scanService.getScanReport(pending.getId()))
                .isInstanceOf(ScanReportNotReadyException.class);
    }

    @Test
    void getScanReportThrowsScanReportNotReadyExceptionWhenReportRowIsMissingDespiteCompletedStatus() {
        Scan scan = completedScan(UUID.randomUUID());
        when(scanRepository.findById(scan.getId())).thenReturn(Optional.of(scan));
        when(scanReportRepository.findByScanId(scan.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scanService.getScanReport(scan.getId()))
                .isInstanceOf(ScanReportNotReadyException.class);
    }

    @Test
    void getScanReportReturnsMappedReportViewWhenFound() {
        Scan scan = completedScan(UUID.randomUUID());
        when(scanRepository.findById(scan.getId())).thenReturn(Optional.of(scan));
        when(scanReportRepository.findByScanId(scan.getId())).thenReturn(Optional.of(reportRow(scan)));

        ScanReportView report = scanService.getScanReport(scan.getId());

        assertThat(report.scanId()).isEqualTo(scan.getId());
    }

    private static ScanGroup scanGroup() {
        return ScanGroup.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .targetType(ScanTarget.ADDRESS)
                .target("0xabc")
                .requestedAt(Instant.now())
                .build();
    }

    private static Scan completedScan(UUID groupId) {
        return completedScan(groupId, Chain.ETHEREUM);
    }

    private static Scan completedScan(UUID groupId, Chain chain) {
        return Scan.builder()
                .id(UUID.randomUUID())
                .groupId(groupId)
                .targetType(ScanTarget.ADDRESS)
                .target("0xabc")
                .chain(chain)
                .status(ScanStage.COMPLETED)
                .source(ScanSource.USER)
                .requestedAt(Instant.now())
                .completedAt(Instant.now())
                .build();
    }

    private static ScanReportRow reportRow(Scan scan) {
        Instant now = Instant.now();
        EvidenceBundle evidence = new AddressEvidence(scan.getTarget(), scan.getChain(), now, null, 0, 0, false, "0",
                List.of(), 0, List.of(), null, new Heuristics(null, null, false, 0, 0));
        return new ScanReportRow(scan.getId(), scan.getTargetType(), scan.getTarget(), scan.getChain(),
                RiskLevel.LOW, 10, "clean wallet", List.of(), List.of(), now, evidence, "deepseek-chat", now);
    }
}
