package com.riskscoring.enrichment.repository;

import com.riskscoring.enrichment.entity.EvidenceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EvidenceRecordRepository extends JpaRepository<EvidenceRecord, UUID> {
}