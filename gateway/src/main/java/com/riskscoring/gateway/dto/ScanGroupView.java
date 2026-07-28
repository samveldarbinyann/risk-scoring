package com.riskscoring.gateway.dto;

import java.util.List;
import java.util.UUID;

public record ScanGroupView(
        UUID groupId,
        String address,
        boolean completed,
        List<ScanGroupChainStatus> chains
) {
}