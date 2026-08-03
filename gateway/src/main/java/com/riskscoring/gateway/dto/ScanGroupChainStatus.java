package com.riskscoring.gateway.dto;

import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.model.Chain;

import java.util.UUID;

public record ScanGroupChainStatus(
        Chain chain,
        UUID scanId,
        ScanStage status
) {
}