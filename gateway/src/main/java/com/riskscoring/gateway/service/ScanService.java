package com.riskscoring.gateway.service;

import com.riskscoring.gateway.dto.ScanAcceptedResponse;
import com.riskscoring.gateway.dto.ScanCreateRequest;
import com.riskscoring.gateway.dto.ScanReportView;
import com.riskscoring.gateway.dto.ScanView;

import java.util.UUID;

public interface ScanService {

    ScanAcceptedResponse requestScan(ScanCreateRequest request);

    ScanView getScan(UUID scanId);

    ScanReportView getScanReport(UUID scanId);
}