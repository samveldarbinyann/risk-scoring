package com.riskscoring.gateway.mapper;

import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.model.Language;
import com.riskscoring.gateway.dto.ScanAcceptedResponse;
import com.riskscoring.gateway.dto.ScanProgressMessage;
import com.riskscoring.gateway.dto.ScanReportView;
import com.riskscoring.gateway.dto.ScanView;
import com.riskscoring.gateway.entity.Scan;
import com.riskscoring.gateway.entity.ScanReportRecord;
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

    public ScanReportView toReportView(ScanReportRecord report) {
        return new ScanReportView(
                report.scanId(),
                report.address(),
                report.chainId(),
                report.riskLevel(),
                report.score(),
                report.explanation(),
                report.decisiveSignals(),
                report.manualChecks(),
                report.model(),
                report.createdAt()
        );
    }
}