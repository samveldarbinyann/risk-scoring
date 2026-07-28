package com.riskscoring.gateway.dto;

import java.util.List;
import java.util.UUID;

public record ScanGroupReportView(
        UUID groupId,
        String address,
        List<ScanReportView> reports
) {
}