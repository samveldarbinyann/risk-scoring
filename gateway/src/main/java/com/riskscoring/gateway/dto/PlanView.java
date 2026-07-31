package com.riskscoring.gateway.dto;

import com.riskscoring.gateway.model.PlanCode;

public record PlanView(
        PlanCode code,
        String name,
        int priceCents,
        String currency,
        int monthlyRequestLimit
) {
}
