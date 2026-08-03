package com.riskscoring.gateway.dto;

import com.riskscoring.common.model.ScanTarget;

import java.util.List;
import java.util.UUID;

public record ScanGroupReportView(
        UUID groupId,
        ScanTarget targetType,
        String target,
        List<ScanReportView> reports
) {
}
