package com.riskscoring.gateway.service;

import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanSource;
import com.riskscoring.gateway.dto.RecentScanGroupView;
import com.riskscoring.gateway.dto.ScanCreateRequest;
import com.riskscoring.gateway.dto.ScanGroupAcceptedResponse;
import com.riskscoring.gateway.dto.ScanGroupReportView;
import com.riskscoring.gateway.dto.ScanGroupView;
import com.riskscoring.gateway.dto.ScanHistoryPageView;
import com.riskscoring.gateway.dto.ScanReportView;
import com.riskscoring.gateway.dto.ScanView;

import java.util.List;
import java.util.UUID;

public interface ScanService {

    ScanGroupAcceptedResponse requestScan(String clientIp, UUID userId, ScanCreateRequest request);

    ScanGroupAcceptedResponse requestApiScan(UUID userId, ScanCreateRequest request);

    List<RecentScanGroupView> getRecentScans(UUID userId);

    ScanHistoryPageView getScanHistory(UUID userId, ScanSource source, int page, int size);

    void ingestMonitorScan(ScanRequested event);

    ScanGroupView getScanGroup(UUID groupId, UUID requesterId);

    ScanGroupReportView getScanGroupReport(UUID groupId, UUID requesterId);

    ScanView getScan(UUID scanId, UUID requesterId);

    ScanReportView getScanReport(UUID scanId, UUID requesterId);

    boolean canAccessGroup(UUID groupId, UUID requesterId);

    boolean canAccessScan(UUID scanId, UUID requesterId);
}
