package com.riskscoring.gateway.repository;

import com.riskscoring.gateway.dto.ScanReportView;

import java.util.Optional;
import java.util.UUID;

public interface ScanReportRepository {

    Optional<ScanReportView> findByScanId(UUID scanId);
}