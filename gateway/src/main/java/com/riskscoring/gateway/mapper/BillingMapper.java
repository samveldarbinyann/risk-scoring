package com.riskscoring.gateway.mapper;

import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.dto.PlanView;
import com.riskscoring.gateway.dto.SubscriptionView;
import com.riskscoring.gateway.entity.Subscription;
import com.riskscoring.gateway.model.BillingPeriods;
import com.riskscoring.gateway.model.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class BillingMapper {

    private final GatewayProperties gatewayProperties;

    public PlanView toPlanView(GatewayProperties.Plan plan) {
        return new PlanView(
                plan.code(),
                plan.priceCents(),
                plan.currency(),
                plan.monthlyRequestLimit()
        );
    }

    public SubscriptionView toView(Subscription subscription) {
        Duration period = gatewayProperties.billing().period();
        Instant now = Instant.now();
        boolean periodElapsed = subscription.getStatus() == SubscriptionStatus.ACTIVE
                && !now.isBefore(subscription.getCurrentPeriodEnd());

        Instant periodStart = periodElapsed
                ? BillingPeriods.startOfPeriodContaining(subscription.getCurrentPeriodStart(), period, now)
                : subscription.getCurrentPeriodStart();
        Instant periodEnd = periodElapsed ? periodStart.plus(period) : subscription.getCurrentPeriodEnd();
        int requestsUsed = periodElapsed ? 0 : subscription.getRequestsUsed();

        return new SubscriptionView(
                subscription.getId(),
                subscription.getPlanCode(),
                subscription.getStatus(),
                subscription.getPriceCents(),
                subscription.getCurrency(),
                subscription.getMonthlyRequestLimit(),
                requestsUsed,
                Math.max(0, subscription.getMonthlyRequestLimit() - requestsUsed),
                periodStart,
                periodEnd,
                subscription.getCreatedAt(),
                subscription.getCanceledAt()
        );
    }
}
