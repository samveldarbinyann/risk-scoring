package com.riskscoring.gateway.dto;

import com.riskscoring.common.event.ScanStage;

import java.util.UUID;

public record ScanGroupChainStatus(
        int chainId,
        UUID scanId,
        ScanStage status
) {
}