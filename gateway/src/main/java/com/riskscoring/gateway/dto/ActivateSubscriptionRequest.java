package com.riskscoring.gateway.dto;

import com.riskscoring.gateway.model.PlanCode;
import jakarta.validation.constraints.NotNull;

public record ActivateSubscriptionRequest(
        @NotNull(message = "{validation.planCode.required}")
        PlanCode planCode
) {
}
