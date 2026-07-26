package com.riskscoring.riskai.mapper;

import com.riskscoring.common.event.SignalsComputed;
import com.riskscoring.common.model.Verdict;
import com.riskscoring.riskai.entity.ScanReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ScanReportMapper {

    private final ObjectMapper objectMapper;

    public ScanReport toEntity(SignalsComputed event, Verdict verdict, String model,
                               String promptVersion, Instant createdAt) {
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
                .model(model)
                .promptVersion(promptVersion)
                .createdAt(createdAt)
                .build();
    }
}
