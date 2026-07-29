package com.riskscoring.riskai.mapper;

import com.riskscoring.common.event.SignalsComputed;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.Verdict;
import com.riskscoring.riskai.entity.ScanReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ScanReportMapper {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public ScanReport toEntity(SignalsComputed event, Verdict verdict, String model,
                               String promptVersion, Instant createdAt) {
        EvidenceBundle evidence = event.evidence();

        return ScanReport.builder()
                .id(UUID.randomUUID())
                .scanId(event.scanId())
                .address(event.address())
                .chainId(event.chainId())
                .riskLevel(verdict.riskLevel())
                .score(verdict.score())
                .explanation(verdict.explanation())
                .decisiveSignals(objectMapper.writeValueAsString(verdict.decisiveSignals()))
                .manualChecks(objectMapper.writeValueAsString(verdict.manualChecks()))
                .balanceWei(evidence.balanceWei())
                .txCount(evidence.txCount())
                .txCount24h(evidence.txCount24h())
                .sampleTruncated(evidence.sampleTruncated())
                .observedAt(evidence.observedAt())
                .tokenBalances(objectMapper.writeValueAsString(evidence.tokenBalances()))
                .model(model)
                .promptVersion(promptVersion)
                .createdAt(createdAt)
                .build();
    }

    public Verdict toVerdict(ScanReport report) {
        return new Verdict(
                report.getRiskLevel(),
                report.getScore(),
                report.getExplanation(),
                objectMapper.readValue(report.getDecisiveSignals(), STRING_LIST),
                objectMapper.readValue(report.getManualChecks(), STRING_LIST)
        );
    }
}
