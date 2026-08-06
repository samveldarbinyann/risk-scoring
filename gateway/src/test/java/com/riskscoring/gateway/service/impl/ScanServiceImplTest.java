package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.gateway.dto.ScanHistoryPageView;
import com.riskscoring.gateway.entity.Scan;
import com.riskscoring.gateway.entity.ScanGroup;
import com.riskscoring.gateway.mapper.ScanMapper;
import com.riskscoring.gateway.repository.ScanGroupRepository;
import com.riskscoring.gateway.repository.ScanReportRepository;
import com.riskscoring.gateway.repository.ScanRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("8dc2f3fd-f443-4481-842e-5d70a6cb4b88");

    @Mock
    private ScanGroupRepository scanGroupRepository;

    @Mock
    private ScanRepository scanRepository;

    @Mock
    private ScanReportRepository scanReportRepository;

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
                new ScanMapper(), null, billingService, rateLimitService, chainService);
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
}
