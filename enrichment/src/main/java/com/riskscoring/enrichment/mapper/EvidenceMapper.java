package com.riskscoring.enrichment.mapper;

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

    public EvidenceRecord toRecord(UUID scanId, EvidenceBundle evidence, Instant createdAt) {
        return EvidenceRecord.builder()
                .id(UUID.randomUUID())
                .scanId(scanId)
                .address(evidence.address())
                .chainId(evidence.chainId())
                .payload(objectMapper.writeValueAsString(evidence))
                .createdAt(createdAt)
                .build();
    }
}