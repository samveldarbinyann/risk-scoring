package com.riskscoring.gateway.dto;

import java.util.List;

public record ScanHistoryPageView(
        List<RecentScanGroupView> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
