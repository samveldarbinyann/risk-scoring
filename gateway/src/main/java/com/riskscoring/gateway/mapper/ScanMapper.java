package com.riskscoring.gateway.mapper;

import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.model.Language;
import com.riskscoring.gateway.dto.ScanGroupAcceptedResponse;
import com.riskscoring.gateway.dto.ScanGroupChainStatus;
import com.riskscoring.gateway.dto.ScanGroupView;
import com.riskscoring.gateway.dto.ScanProgressMessage;
import com.riskscoring.gateway.dto.ScanReportView;
import com.riskscoring.gateway.dto.ScanView;
import com.riskscoring.gateway.entity.Scan;
import com.riskscoring.gateway.entity.ScanGroup;
import com.riskscoring.gateway.repository.ScanReportRow;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class ScanMapper {

    private static final Set<ScanStage> TERMINAL_STAGES = Set.of(ScanStage.COMPLETED, ScanStage.FAILED);

    public ScanRequested toEvent(Scan scan, Language language) {
        return new ScanRequested(
                scan.getId(),
                scan.getTargetType(),
                scan.getTarget(),
                scan.getChain(),
                scan.getRequestedAt(),
                scan.getSource(),
                language
        );
    }

    public ScanProgressMessage toProgressMessage(ScanProgress event) {
        return new ScanProgressMessage(event.scanId(), event.stage(), event.message(), event.at());
    }

    public ScanGroupAcceptedResponse toGroupAcceptedResponse(ScanGroup group, List<Scan> scans) {
        return new ScanGroupAcceptedResponse(
                group.getId(),
                group.getTargetType(),
                group.getTarget(),
                scans.stream().map(Scan::getChain).toList()
        );
    }

    public ScanGroupView toGroupView(UUID groupId, List<Scan> scans) {
        List<ScanGroupChainStatus> chains = scans.stream()
                .map(scan -> new ScanGroupChainStatus(scan.getChain(), scan.getId(), scan.getStatus()))
                .toList();

        boolean completed = scans.stream().allMatch(scan -> TERMINAL_STAGES.contains(scan.getStatus()));
        Scan first = scans.getFirst();

        return new ScanGroupView(groupId, first.getTargetType(), first.getTarget(), completed, chains);
    }

    public ScanView toView(Scan scan) {
        return new ScanView(
                scan.getId(),
                scan.getTargetType(),
                scan.getTarget(),
                scan.getChain(),
                scan.getStatus(),
                scan.getSource(),
                scan.getRequestedAt(),
                scan.getCompletedAt()
        );
    }

    public ScanReportView toReportView(ScanReportRow row) {
        return new ScanReportView(
                row.scanId(),
                row.targetType(),
                row.target(),
                row.chain(),
                row.riskLevel(),
                row.score(),
                row.explanation(),
                row.decisiveSignals(),
                row.manualChecks(),
                row.observedAt(),
                row.evidence(),
                row.model(),
                row.createdAt()
        );
    }
}
