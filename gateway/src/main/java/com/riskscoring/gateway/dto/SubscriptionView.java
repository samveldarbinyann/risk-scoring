package com.riskscoring.gateway.dto;

import com.riskscoring.gateway.model.PlanCode;
import com.riskscoring.gateway.model.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionView(
        UUID id,
        PlanCode planCode,
        SubscriptionStatus status,
        int priceCents,
        String currency,
        int monthlyRequestLimit,
        int requestsUsed,
        int requestsRemaining,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        Instant createdAt,
        Instant canceledAt,
        String paymentAddress,
        BigDecimal paymentAmount,
        Instant paymentExpiresAt,
        String paymentUri
) {
}
