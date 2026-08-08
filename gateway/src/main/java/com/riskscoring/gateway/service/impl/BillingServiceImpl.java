package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.event.UsdtPaymentDetected;
import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.dto.PlanView;
import com.riskscoring.gateway.dto.SubscriptionView;
import com.riskscoring.gateway.entity.Subscription;
import com.riskscoring.gateway.exception.ApiException;
import com.riskscoring.gateway.exception.NoActiveSubscriptionException;
import com.riskscoring.gateway.exception.PaymentAmountConflictException;
import com.riskscoring.gateway.exception.PaymentNotConfiguredException;
import com.riskscoring.gateway.exception.QuotaExceededException;
import com.riskscoring.gateway.exception.SubscriptionAlreadyActiveException;
import com.riskscoring.gateway.exception.SubscriptionNotFoundException;
import com.riskscoring.gateway.mapper.BillingMapper;
import com.riskscoring.gateway.model.BillingPeriods;
import com.riskscoring.gateway.model.PlanCode;
import com.riskscoring.gateway.model.SubscriptionStatus;
import com.riskscoring.gateway.repository.SubscriptionRepository;
import com.riskscoring.gateway.service.ApiKeyService;
import com.riskscoring.gateway.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingServiceImpl implements BillingService {

    private static final Set<SubscriptionStatus> LIVE_STATUSES =
            EnumSet.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PENDING_PAYMENT);
    private static final Set<SubscriptionStatus> CONFIRMABLE_STATUSES =
            EnumSet.of(SubscriptionStatus.PENDING_PAYMENT, SubscriptionStatus.EXPIRED);
    private static final int MAX_AMOUNT_GENERATION_ATTEMPTS = 20;

    private final SubscriptionRepository subscriptionRepository;
    private final BillingMapper billingMapper;
    private final GatewayProperties gatewayProperties;
    // ObjectProvider breaks the ApiKeyService ↔ BillingService constructor cycle.
    private final ObjectProvider<ApiKeyService> apiKeyService;

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
                .map(live -> activateLiveSubscription(live, plan, now))
                .orElseGet(() -> subscriptionRepository.save(newSubscription(userId, plan, now)));

        return billingMapper.toView(subscription);
    }

    @Override
    @Transactional
    public void confirmPaymentFromChain(UsdtPaymentDetected event) {
        BigDecimal amount = event.amount().setScale(6, RoundingMode.HALF_UP);
        subscriptionRepository.findByStatusInAndPaymentAmount(CONFIRMABLE_STATUSES, amount).stream()
                .filter(subscription -> subscription.getPaymentExpiresAt() != null
                        && event.blockTimestamp().isBefore(subscription.getPaymentExpiresAt()))
                .findFirst()
                .ifPresentOrElse(
                        subscription -> activateFromChainPayment(subscription, event),
                        () -> log.info("No PENDING_PAYMENT/EXPIRED subscription matched on-chain USDT payment amount={} txHash={}",
                                amount, event.txHash()));
    }

    private void activateFromChainPayment(Subscription subscription, UsdtPaymentDetected event) {
        Instant now = Instant.now();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setRequestsUsed(0);
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(now.plus(gatewayProperties.billing().period()));
        subscription.setPaidTxHash(event.txHash());
        subscription.setUpdatedAt(now);
        log.info("Activated subscription {} via on-chain USDT payment txHash={}", subscription.getId(), event.txHash());
    }

    @Override
    @Scheduled(fixedDelayString = "${gateway.billing.payment.reaper-fixed-delay}")
    @Transactional
    public void expireOverduePayments() {
        Instant now = Instant.now();
        List<Subscription> overdue = subscriptionRepository.findByStatusAndPaymentExpiresAtBefore(
                SubscriptionStatus.PENDING_PAYMENT, now);
        for (Subscription subscription : overdue) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscription.setUpdatedAt(now);
        }
        if (!overdue.isEmpty()) {
            subscriptionRepository.saveAll(overdue);
            log.info("Expired {} overdue PENDING_PAYMENT subscription(s)", overdue.size());
        }
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
        apiKeyService.getObject().revokeAllActive(userId);
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

    private Subscription activateLiveSubscription(Subscription subscription, GatewayProperties.Plan plan, Instant now) {
        if (plan.code() == PlanCode.FREE) {
            return activateFreeSubscription(subscription, plan, now);
        }

        return repriceUnpaid(subscription, plan, now);
    }

    private Subscription newSubscription(UUID userId, GatewayProperties.Plan plan, Instant now) {
        return plan.code() == PlanCode.FREE
                ? newActiveSubscription(userId, plan, now)
                : newPendingSubscription(userId, plan, now);
    }

    private Subscription activateFreeSubscription(Subscription subscription, GatewayProperties.Plan plan, Instant now) {
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            throw new SubscriptionAlreadyActiveException();
        }

        subscription.setPlanCode(plan.code());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPriceCents(plan.priceCents());
        subscription.setCurrency(plan.currency());
        subscription.setMonthlyRequestLimit(plan.monthlyRequestLimit());
        subscription.setRequestsUsed(0);
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(now.plus(gatewayProperties.billing().period()));
        subscription.setUpdatedAt(now);
        return subscription;
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
        applyPaymentRequest(subscription, plan, now);
        return subscription;
    }

    private Subscription newPendingSubscription(UUID userId, GatewayProperties.Plan plan, Instant now) {
        Subscription subscription = Subscription.builder()
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
        applyPaymentRequest(subscription, plan, now);
        return subscription;
    }

    private void applyPaymentRequest(Subscription subscription, GatewayProperties.Plan plan, Instant now) {
        GatewayProperties.Payment paymentConfig = gatewayProperties.billing().payment();
        if (paymentConfig.address() == null || paymentConfig.address().isBlank()) {
            throw new PaymentNotConfiguredException();
        }

        BigDecimal basePrice = BigDecimal.valueOf(plan.priceCents(), 2);
        subscription.setPaymentAddress(paymentConfig.address());
        subscription.setPaymentAmount(generateUniquePaymentAmount(basePrice, paymentConfig));
        subscription.setPaymentExpiresAt(now.plus(paymentConfig.window()));
        subscription.setPaidTxHash(null);

        try {
            subscriptionRepository.saveAndFlush(subscription);
        } catch (DataIntegrityViolationException e) {
            throw new PaymentAmountConflictException(e);
        }
    }

    private BigDecimal generateUniquePaymentAmount(BigDecimal basePrice, GatewayProperties.Payment paymentConfig) {
        for (int attempt = 0; attempt < MAX_AMOUNT_GENERATION_ATTEMPTS; attempt++) {
            int tailMicroUsdt = ThreadLocalRandom.current()
                    .nextInt(paymentConfig.tailMinMicroUsdt(), paymentConfig.tailMaxMicroUsdt() + 1);
            BigDecimal amount = basePrice.add(BigDecimal.valueOf(tailMicroUsdt, 6));
            if (!subscriptionRepository.existsByStatusAndPaymentAmount(SubscriptionStatus.PENDING_PAYMENT, amount)) {
                return amount;
            }
        }
        throw new IllegalStateException(
                "Could not generate a unique payment amount after " + MAX_AMOUNT_GENERATION_ATTEMPTS + " attempts");
    }

    private Subscription newActiveSubscription(UUID userId, GatewayProperties.Plan plan, Instant now) {
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
                .currentPeriodEnd(now.plus(gatewayProperties.billing().period()))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
