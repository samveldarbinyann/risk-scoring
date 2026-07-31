package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.dto.PlanView;
import com.riskscoring.gateway.dto.SubscriptionView;
import com.riskscoring.gateway.entity.Subscription;
import com.riskscoring.gateway.exception.ApiException;
import com.riskscoring.gateway.exception.NoActiveSubscriptionException;
import com.riskscoring.gateway.exception.QuotaExceededException;
import com.riskscoring.gateway.exception.SubscriptionAlreadyActiveException;
import com.riskscoring.gateway.exception.SubscriptionNotFoundException;
import com.riskscoring.gateway.exception.SubscriptionNotPendingException;
import com.riskscoring.gateway.mapper.BillingMapper;
import com.riskscoring.gateway.model.BillingPeriods;
import com.riskscoring.gateway.model.PlanCode;
import com.riskscoring.gateway.model.SubscriptionStatus;
import com.riskscoring.gateway.repository.SubscriptionRepository;
import com.riskscoring.gateway.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private static final Set<SubscriptionStatus> LIVE_STATUSES =
            EnumSet.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PENDING_PAYMENT);

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
    @Transactional(readOnly = true)
    public SubscriptionView getSubscription(UUID userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatusIn(userId, LIVE_STATUSES)
                .or(() -> subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId))
                .orElseThrow(SubscriptionNotFoundException::new);

        return billingMapper.toView(subscription);
    }

    @Override
    @Transactional
    public SubscriptionView activate(UUID userId, PlanCode planCode) {
        GatewayProperties.Plan plan = gatewayProperties.billing().requirePlan(planCode);
        Instant now = Instant.now();

        Subscription subscription = subscriptionRepository.findByUserIdAndStatusIn(userId, LIVE_STATUSES)
                .map(live -> repriceUnpaid(live, plan, now))
                .orElseGet(() -> subscriptionRepository.save(newUnpaidSubscription(userId, plan, now)));

        return billingMapper.toView(subscription);
    }

    @Override
    @Transactional
    public SubscriptionView confirmPayment(UUID userId, UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(SubscriptionNotFoundException::new);
        if (!subscription.getUserId().equals(userId)) {
            throw new SubscriptionNotFoundException();
        }
        if (subscription.getStatus() != SubscriptionStatus.PENDING_PAYMENT) {
            throw new SubscriptionNotPendingException(subscriptionId);
        }

        Instant now = Instant.now();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setRequestsUsed(0);
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(now.plus(gatewayProperties.billing().period()));
        subscription.setUpdatedAt(now);
        return billingMapper.toView(subscription);
    }

    @Override
    @Transactional
    public SubscriptionView cancel(UUID userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatusIn(userId, LIVE_STATUSES)
                .orElseThrow(NoActiveSubscriptionException::new);

        Instant now = Instant.now();
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCanceledAt(now);
        subscription.setUpdatedAt(now);
        return billingMapper.toView(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireActiveSubscription(UUID userId) {
        subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(NoActiveSubscriptionException::new);
    }

    @Override
    @Transactional
    public void chargeQuota(UUID userId, int units) {
        if (units <= 0) {
            throw new IllegalArgumentException("units must be positive");
        }

        Subscription subscription = resolveActiveSubscription(userId);
        int charged = subscriptionRepository.tryCharge(
                subscription.getId(), units, Instant.now(), SubscriptionStatus.ACTIVE);
        if (charged == 0) {
            throw chargeFailure(subscription.getId(), units);
        }
    }

    private ApiException chargeFailure(UUID subscriptionId, int units) {
        Subscription current = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(SubscriptionNotFoundException::new);

        return current.getStatus() == SubscriptionStatus.ACTIVE
                ? new QuotaExceededException(current.getMonthlyRequestLimit(), current.getRequestsUsed(), units)
                : new NoActiveSubscriptionException();
    }

    private Subscription resolveActiveSubscription(UUID userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(NoActiveSubscriptionException::new);

        return rollPeriodIfNeeded(subscription);
    }

    private Subscription rollPeriodIfNeeded(Subscription subscription) {
        Instant now = Instant.now();
        if (now.isBefore(subscription.getCurrentPeriodEnd())) {
            return subscription;
        }

        Duration period = gatewayProperties.billing().period();
        Instant periodStart = BillingPeriods.startOfPeriodContaining(
                subscription.getCurrentPeriodStart(), period, now);
        subscriptionRepository.rollPeriod(
                subscription.getId(), periodStart, periodStart.plus(period), now, SubscriptionStatus.ACTIVE);

        return subscriptionRepository.findById(subscription.getId())
                .orElseThrow(SubscriptionNotFoundException::new);
    }

    private Subscription newUnpaidSubscription(UUID userId, GatewayProperties.Plan plan, Instant now) {
        return Subscription.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .planCode(plan.code())
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .priceCents(plan.priceCents())
                .currency(plan.currency())
                .monthlyRequestLimit(plan.monthlyRequestLimit())
                .requestsUsed(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Subscription repriceUnpaid(Subscription subscription, GatewayProperties.Plan plan, Instant now) {
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            throw new SubscriptionAlreadyActiveException();
        }

        subscription.setPlanCode(plan.code());
        subscription.setPriceCents(plan.priceCents());
        subscription.setCurrency(plan.currency());
        subscription.setMonthlyRequestLimit(plan.monthlyRequestLimit());
        subscription.setUpdatedAt(now);
        return subscription;
    }
}
