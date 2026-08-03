package com.riskscoring.gateway.dto;

import com.riskscoring.common.model.ScanTarget;

import java.util.List;
import java.util.UUID;

public record ScanGroupAcceptedResponse(
        UUID groupId,
        ScanTarget targetType,
        String target,
        List<Integer> chainIds
) {
}
