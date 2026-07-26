package com.riskscoring.gateway.dto;

import com.riskscoring.common.event.ScanStage;

import java.util.UUID;

public record ScanAcceptedResponse(
        UUID scanId,
        ScanStage status
) {
}