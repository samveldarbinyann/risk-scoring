package com.riskscoring.gateway.mapper;

import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.model.Language;
import com.riskscoring.gateway.dto.ScanAcceptedResponse;
import com.riskscoring.gateway.dto.ScanProgressMessage;
import com.riskscoring.gateway.dto.ScanReportView;
import com.riskscoring.gateway.dto.ScanView;
import com.riskscoring.gateway.entity.Scan;
import com.riskscoring.gateway.repository.ScanReportRow;
import org.springframework.stereotype.Component;

@Component
public class ScanMapper {

    public ScanRequested toEvent(Scan scan, Language language) {
        return new ScanRequested(
                scan.getId(),
                scan.getAddress(),
                scan.getChainId(),
                scan.getRequestedAt(),
                scan.getSource(),
                language
        );
    }

    public ScanProgressMessage toProgressMessage(ScanProgress event) {
        return new ScanProgressMessage(event.scanId(), event.stage(), event.message(), event.at());
    }

    public ScanAcceptedResponse toAcceptedResponse(Scan scan) {
        return new ScanAcceptedResponse(scan.getId(), scan.getStatus());
    }

    public ScanView toView(Scan scan) {
        return new ScanView(
                scan.getId(),
                scan.getAddress(),
                scan.getChainId(),
                scan.getStatus(),
                scan.getSource(),
                scan.getRequestedAt(),
                scan.getCompletedAt()
        );
    }

    public ScanReportView toReportView(ScanReportRow row) {
        return new ScanReportView(
                row.scanId(),
                row.address(),
                row.chainId(),
                row.riskLevel(),
                row.score(),
                row.explanation(),
                row.decisiveSignals(),
                row.manualChecks(),
                row.model(),
                row.createdAt()
        );
    }
}