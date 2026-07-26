package com.riskscoring.riskai.repository;

import com.riskscoring.riskai.entity.ScanReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScanReportRepository extends JpaRepository<ScanReport, UUID> {

    Optional<ScanReport> findByScanId(UUID scanId);
}
