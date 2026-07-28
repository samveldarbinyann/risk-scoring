package com.riskscoring.gateway.repository;

import com.riskscoring.gateway.entity.ScanReportRecord;

import java.util.Optional;
import java.util.UUID;

public interface ScanReportRepository {

    Optional<ScanReportRecord> findByScanId(UUID scanId);
}