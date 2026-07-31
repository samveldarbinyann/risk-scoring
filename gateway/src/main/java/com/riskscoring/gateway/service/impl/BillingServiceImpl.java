package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.dto.PlanView;
import com.riskscoring.gateway.dto.SubscriptionView;
import com.riskscoring.gateway.entity.Subscription;
import com.riskscoring.gateway.exception.NoActiveSubscriptionException;
import com.riskscoring.gateway.exception.QuotaExceededException;
import com.riskscoring.gateway.exception.SubscriptionNotFoundException;
import com.riskscoring.gateway.mapper.BillingMapper;
import com.riskscoring.gateway.model.PlanCode;
import com.riskscoring.gateway.model.SubscriptionStatus;
import com.riskscoring.gateway.repository.SubscriptionRepository;
import com.riskscoring.gateway.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final SubscriptionRepository subscriptionRepository;
    private final BillingMapper billingMapper;
    private final GatewayProperties gatewayProperties;

    @Override
    public List<PlanView> listPlans() {
        return gatewayProperties.billing().plans().stream()
                .map(billingMapper::toPlanView)
                .toList();
    }

    @Override
    @Transactional
    public SubscriptionView getSubscription(UUID userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .or(() -> subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId))
                .orElseThrow(SubscriptionNotFoundException::new);

        rollPeriodIfNeeded(subscription);
        return billingMapper.toView(subscription);
    }

    @Override
    @Transactional
    public SubscriptionView activate(UUID userId, PlanCode planCode) {
        GatewayProperties.Plan plan = gatewayProperties.billing().requirePlan(planCode);
        Instant now = Instant.now();

        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .map(active -> applyPlan(active, plan, now))
                .orElseGet(() -> subscriptionRepository.save(createSubscription(userId, plan, now)));

        return billingMapper.toView(subscription);
    }

    @Override
    @Transactional
    public SubscriptionView cancel(UUID userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(NoActiveSubscriptionException::new);

        Instant now = Instant.now();
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCanceledAt(now);
        subscription.setUpdatedAt(now);
        return billingMapper.toView(subscription);
    }

    @Override
    @Transactional
    public void requireActiveSubscription(UUID userId) {
        resolveActiveSubscription(userId);
    }

    @Override
    @Transactional
    public void chargeQuota(UUID userId, int units) {
        if (units <= 0) {
            throw new IllegalArgumentException("units must be positive");
        }

        Subscription subscription = resolveActiveSubscription(userId);
        int updated = subscriptionRepository.tryCharge(
                subscription.getId(), units, Instant.now(), SubscriptionStatus.ACTIVE);
        if (updated == 0) {
            throw new QuotaExceededException(
                    subscription.getMonthlyRequestLimit(), subscription.getRequestsUsed(), units);
        }
    }

    private Subscription resolveActiveSubscription(UUID userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(NoActiveSubscriptionException::new);
        rollPeriodIfNeeded(subscription);
        return subscription;
    }

    private void rollPeriodIfNeeded(Subscription subscription) {
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            return;
        }

        Instant now = Instant.now();
        if (now.isBefore(subscription.getCurrentPeriodEnd())) {
            return;
        }

        Duration period = gatewayProperties.billing().period();
        long elapsedPeriods = Duration.between(subscription.getCurrentPeriodStart(), now).dividedBy(period);
        Instant periodStart = subscription.getCurrentPeriodStart().plus(period.multipliedBy(elapsedPeriods));

        subscription.setCurrentPeriodStart(periodStart);
        subscription.setCurrentPeriodEnd(periodStart.plus(period));
        subscription.setRequestsUsed(0);
        subscription.setUpdatedAt(now);
    }

    private Subscription createSubscription(UUID userId, GatewayProperties.Plan plan, Instant now) {
        Instant periodEnd = now.plus(gatewayProperties.billing().period());
        return Subscription.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .planCode(plan.code())
                .status(SubscriptionStatus.ACTIVE)
                .priceCents(plan.priceCents())
                .currency(plan.currency())
                .monthlyRequestLimit(plan.monthlyRequestLimit())
                .requestsUsed(0)
                .currentPeriodStart(now)
                .currentPeriodEnd(periodEnd)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Subscription applyPlan(Subscription subscription, GatewayProperties.Plan plan, Instant now) {
        rollPeriodIfNeeded(subscription);
        subscription.setPlanCode(plan.code());
        subscription.setPriceCents(plan.priceCents());
        subscription.setCurrency(plan.currency());
        subscription.setMonthlyRequestLimit(plan.monthlyRequestLimit());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCanceledAt(null);
        subscription.setUpdatedAt(now);
        return subscription;
    }

}
