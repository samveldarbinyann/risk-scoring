package com.riskscoring.gateway.dto;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.ChainSupport;
import com.riskscoring.common.model.ScanTarget;

public record ChainCandidate(
        Chain chain,
        ChainFamily family,
        String displayName,
        String nativeSymbol,
        ScanTarget targetType,
        ChainSupport support,
        String normalizedTarget
) {
}
