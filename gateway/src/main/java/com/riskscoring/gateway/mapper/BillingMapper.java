package com.riskscoring.gateway.mapper;

import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.dto.PlanView;
import com.riskscoring.gateway.dto.SubscriptionView;
import com.riskscoring.gateway.entity.Subscription;
import org.springframework.stereotype.Component;

@Component
public class BillingMapper {

    public PlanView toPlanView(GatewayProperties.Plan plan) {
        return new PlanView(
                plan.code(),
                plan.name(),
                plan.priceCents(),
                plan.currency(),
                plan.monthlyRequestLimit()
        );
    }

    public SubscriptionView toView(Subscription subscription) {
        int remaining = Math.max(0, subscription.getMonthlyRequestLimit() - subscription.getRequestsUsed());
        return new SubscriptionView(
                subscription.getId(),
                subscription.getPlanCode(),
                subscription.getStatus(),
                subscription.getPriceCents(),
                subscription.getCurrency(),
                subscription.getMonthlyRequestLimit(),
                subscription.getRequestsUsed(),
                remaining,
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.getCreatedAt(),
                subscription.getCanceledAt()
        );
    }
}
