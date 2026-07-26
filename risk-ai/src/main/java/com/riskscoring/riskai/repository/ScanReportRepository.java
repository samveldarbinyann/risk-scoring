package com.riskscoring.riskai.repository;

import com.riskscoring.riskai.entity.ScanReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScanReportRepository extends JpaRepository<ScanReport, UUID> {

    boolean existsByScanId(UUID scanId);
}
