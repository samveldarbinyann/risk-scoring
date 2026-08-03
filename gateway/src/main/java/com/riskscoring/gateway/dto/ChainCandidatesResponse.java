package com.riskscoring.gateway.dto;

import com.riskscoring.common.model.ScanTarget;

import java.util.List;

public record ChainCandidatesResponse(
        ScanTarget targetType,
        String target,
        List<Integer> chainIds
) {
}
