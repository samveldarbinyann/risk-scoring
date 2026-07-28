package com.riskscoring.gateway.dto;

import java.util.List;
import java.util.UUID;

public record ScanGroupAcceptedResponse(
        UUID groupId,
        String address,
        List<Integer> chainIds
) {
}