package com.riskscoring.gateway.repository;

import java.util.Optional;
import java.util.UUID;

public interface ScanReportRepository {

    Optional<ScanReportRow> findByScanId(UUID scanId);
}