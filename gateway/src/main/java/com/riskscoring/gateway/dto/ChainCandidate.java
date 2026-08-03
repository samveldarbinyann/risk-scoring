package com.riskscoring.gateway.dto;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ScanTarget;

public record ChainCandidate(
        Chain chain,
        ScanTarget targetType,
        String normalizedTarget
) {
}
