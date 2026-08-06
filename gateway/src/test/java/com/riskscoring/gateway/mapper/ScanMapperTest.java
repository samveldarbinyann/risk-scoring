package com.riskscoring.gateway.mapper;

import com.riskscoring.common.event.ScanProgress;
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
import com.riskscoring.gateway.dto.ScanGroupAcceptedResponse;
import com.riskscoring.gateway.dto.ScanGroupView;
import com.riskscoring.gateway.dto.ScanProgressMessage;
import com.riskscoring.gateway.dto.ScanReportView;
import com.riskscoring.gateway.dto.ScanView;
import com.riskscoring.gateway.entity.Scan;
import com.riskscoring.gateway.entity.ScanGroup;
import com.riskscoring.gateway.repository.ScanReportRow;
import com.riskscoring.gateway.repository.ScanRiskSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScanMapperTest {

    private static final Instant NOW = Instant.parse("2024-06-01T00:00:00Z");

    private final ScanMapper mapper = new ScanMapper();

    @Test
    void toEventMapsFieldsFromScanAndArguments() {
        Scan scan = scan(UUID.randomUUID(), ScanStage.PENDING, ScanSource.USER, Chain.ETHEREUM);
        UUID userId = UUID.randomUUID();

        ScanRequested event = mapper.toEvent(scan, Language.RU, userId);

        assertThat(event).isEqualTo(new ScanRequested(scan.getId(), scan.getTargetType(), scan.getTarget(),
                scan.getChain(), scan.getRequestedAt(), scan.getSource(), Language.RU, userId));
    }

    @Test
    void toProgressMessageMapsFieldsFromEventAndMessage() {
        ScanProgress event = new ScanProgress(UUID.randomUUID(), ScanStage.ANALYZING,
                "console.message.analyzing", List.of(), Language.EN, NOW);

        ScanProgressMessage message = mapper.toProgressMessage(event, "Analyzing...");

        assertThat(message).isEqualTo(new ScanProgressMessage(event.scanId(), ScanStage.ANALYZING, "Analyzing...", NOW));
    }

    @Test
    void toGroupAcceptedResponseCollectsChainsFromScans() {
        ScanGroup group = scanGroup();
        Scan scanA = scan(UUID.randomUUID(), ScanStage.PENDING, ScanSource.USER, Chain.ETHEREUM);
        Scan scanB = scan(UUID.randomUUID(), ScanStage.PENDING, ScanSource.USER, Chain.BITCOIN);

        ScanGroupAcceptedResponse response = mapper.toGroupAcceptedResponse(group, List.of(scanA, scanB));

        assertThat(response.chains()).containsExactly(Chain.ETHEREUM, Chain.BITCOIN);
        assertThat(response.groupId()).isEqualTo(group.getId());
    }

    @Test
    void toGroupViewMarksCompletedWhenAllScansAreTerminal() {
        ScanGroup group = scanGroup();
        Scan completed = scan(UUID.randomUUID(), ScanStage.COMPLETED, ScanSource.USER, Chain.ETHEREUM);
        Scan failed = scan(UUID.randomUUID(), ScanStage.FAILED, ScanSource.USER, Chain.BITCOIN);

        ScanGroupView view = mapper.toGroupView(group.getId(), List.of(completed, failed));

        assertThat(view.completed()).isTrue();
        assertThat(view.chains()).hasSize(2);
    }

    @Test
    void toGroupViewMarksNotCompletedWhenAnyScanIsPending() {
        ScanGroup group = scanGroup();
        Scan completed = scan(UUID.randomUUID(), ScanStage.COMPLETED, ScanSource.USER, Chain.ETHEREUM);
        Scan pending = scan(UUID.randomUUID(), ScanStage.PENDING, ScanSource.USER, Chain.BITCOIN);

        ScanGroupView view = mapper.toGroupView(group.getId(), List.of(completed, pending));

        assertThat(view.completed()).isFalse();
    }

    @Test
    void toViewMapsAllFieldsFromScan() {
        Scan scan = scan(UUID.randomUUID(), ScanStage.COMPLETED, ScanSource.API, Chain.ETHEREUM);

        ScanView view = mapper.toView(scan);

        assertThat(view).isEqualTo(new ScanView(scan.getId(), scan.getTargetType(), scan.getTarget(), scan.getChain(),
                scan.getStatus(), scan.getSource(), scan.getRequestedAt(), scan.getCompletedAt()));
    }

    @Test
    void toRecentScanViewSelectsHighestRiskLevelAmongReportedScans() {
        ScanGroup group = scanGroup();
        Scan low = scan(UUID.randomUUID(), ScanStage.COMPLETED, ScanSource.USER, Chain.ETHEREUM);
        Scan critical = scan(UUID.randomUUID(), ScanStage.COMPLETED, ScanSource.USER, Chain.BITCOIN);
        Map<UUID, ScanRiskSummary> riskByScanId = Map.of(
                low.getId(), new ScanRiskSummary(low.getId(), RiskLevel.LOW, 10),
                critical.getId(), new ScanRiskSummary(critical.getId(), RiskLevel.CRITICAL, 95));

        RecentScanGroupView view = mapper.toRecentScanView(group, List.of(low, critical), riskByScanId);

        assertThat(view.worstRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(view.worstScore()).isEqualTo(95);
    }

    @Test
    void toRecentScanViewReturnsNullRiskWhenNoScanHasAReportYet() {
        ScanGroup group = scanGroup();
        Scan pending = scan(UUID.randomUUID(), ScanStage.PENDING, ScanSource.USER, Chain.ETHEREUM);

        RecentScanGroupView view = mapper.toRecentScanView(group, List.of(pending), Map.of());

        assertThat(view.worstRiskLevel()).isNull();
        assertThat(view.worstScore()).isNull();
        assertThat(view.completed()).isFalse();
    }

    @Test
    void toRecentScanViewUsesSourceOfFirstScan() {
        ScanGroup group = scanGroup();
        Scan scan = scan(UUID.randomUUID(), ScanStage.COMPLETED, ScanSource.MONITOR, Chain.ETHEREUM);

        RecentScanGroupView view = mapper.toRecentScanView(group, List.of(scan), Map.of());

        assertThat(view.source()).isEqualTo(ScanSource.MONITOR);
    }

    @Test
    void toReportViewMapsAllFieldsFromRow() {
        EvidenceBundle evidence = new AddressEvidence("0xabc", Chain.ETHEREUM, NOW, null, 0, 0, false, "0",
                List.of(), 0, List.of(), null, new Heuristics(null, null, false, 0, 0));
        ScanReportRow row = new ScanReportRow(UUID.randomUUID(), ScanTarget.ADDRESS, "0xabc", Chain.ETHEREUM,
                RiskLevel.LOW, 10, "clean wallet", List.of("a"), List.of(), NOW, evidence, "deepseek-chat", NOW);

        ScanReportView view = mapper.toReportView(row);

        assertThat(view).isEqualTo(new ScanReportView(row.scanId(), row.targetType(), row.target(), row.chain(),
                row.riskLevel(), row.score(), row.explanation(), row.decisiveSignals(), row.manualChecks(),
                row.observedAt(), row.evidence(), row.model(), row.createdAt()));
    }

    private static Scan scan(UUID id, ScanStage status, ScanSource source, Chain chain) {
        return Scan.builder()
                .id(id)
                .groupId(UUID.randomUUID())
                .targetType(ScanTarget.ADDRESS)
                .target("0xabc")
                .chain(chain)
                .status(status)
                .source(source)
                .requestedAt(NOW)
                .build();
    }

    private static ScanGroup scanGroup() {
        return ScanGroup.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .targetType(ScanTarget.ADDRESS)
                .target("0xabc")
                .requestedAt(NOW)
                .build();
    }
}
