package com.riskscoring.enrichment.mapper;

import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.enrichment.entity.EvidenceRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EvidenceMapper {

    private final ObjectMapper objectMapper;

    public EvidenceRecord toRecord(ChainFetched event, EvidenceBundle evidence, Instant createdAt) {
        return EvidenceRecord.builder()
                .id(UUID.randomUUID())
                .scanId(event.scanId())
                .targetType(event.targetType())
                .target(evidence.target())
                .chainId(evidence.chainId())
                .payload(objectMapper.writeValueAsString(evidence))
                .createdAt(createdAt)
                .build();
    }
}
