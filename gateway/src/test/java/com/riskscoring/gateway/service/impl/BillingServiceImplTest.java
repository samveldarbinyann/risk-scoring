package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.dto.SubscriptionView;
import com.riskscoring.gateway.entity.Subscription;
import com.riskscoring.gateway.exception.SubscriptionAlreadyActiveException;
import com.riskscoring.gateway.mapper.BillingMapper;
import com.riskscoring.gateway.model.PlanCode;
import com.riskscoring.gateway.model.SubscriptionStatus;
import com.riskscoring.gateway.repository.SubscriptionRepository;
import com.riskscoring.gateway.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("8dc2f3fd-f443-4481-842e-5d70a6cb4b88");

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private BillingMapper billingMapper;

    @Mock
    private ObjectProvider<ApiKeyService> apiKeyService;

    @Captor
    private ArgumentCaptor<Subscription> subscriptionCaptor;

    private BillingServiceImpl billingService;

    @BeforeEach
    void setUp() {
        billingService = new BillingServiceImpl(subscriptionRepository, billingMapper, gatewayProperties(), apiKeyService);
    }

    @Test
    void activateCreatesActiveFreeSubscriptionImmediately() {
        stubSubscriptionView();
        when(subscriptionRepository.findByUserIdAndStatusIn(any(), any())).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        billingService.activate(USER_ID, PlanCode.FREE);

        verify(subscriptionRepository).save(subscriptionCaptor.capture());
        Subscription subscription = subscriptionCaptor.getValue();
        assertThat(subscription.getUserId()).isEqualTo(USER_ID);
        assertThat(subscription.getPlanCode()).isEqualTo(PlanCode.FREE);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getPriceCents()).isZero();
        assertThat(subscription.getMonthlyRequestLimit()).isEqualTo(10);
        assertThat(subscription.getRequestsUsed()).isZero();
        assertThat(subscription.getCurrentPeriodStart()).isNotNull();
        assertThat(subscription.getCurrentPeriodEnd()).isEqualTo(subscription.getCurrentPeriodStart().plus(Duration.ofDays(30)));
    }

    @Test
    void activatePromotesPendingSubscriptionToFreeImmediately() {
        stubSubscriptionView();
        Subscription pending = Subscription.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .planCode(PlanCode.STARTER)
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .priceCents(2_000)
                .currency("USD")
                .monthlyRequestLimit(1_000)
                .requestsUsed(7)
                .createdAt(Instant.now().minusSeconds(300))
                .updatedAt(Instant.now().minusSeconds(300))
                .build();
        when(subscriptionRepository.findByUserIdAndStatusIn(any(), any())).thenReturn(Optional.of(pending));

        billingService.activate(USER_ID, PlanCode.FREE);

        verify(subscriptionRepository, never()).save(any());
        assertThat(pending.getPlanCode()).isEqualTo(PlanCode.FREE);
        assertThat(pending.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(pending.getPriceCents()).isZero();
        assertThat(pending.getMonthlyRequestLimit()).isEqualTo(10);
        assertThat(pending.getRequestsUsed()).isZero();
        assertThat(pending.getCurrentPeriodStart()).isNotNull();
        assertThat(pending.getCurrentPeriodEnd()).isEqualTo(pending.getCurrentPeriodStart().plus(Duration.ofDays(30)));
    }

    @Test
    void activateKeepsPaidPlansPending() {
        stubSubscriptionView();
        when(subscriptionRepository.findByUserIdAndStatusIn(any(), any())).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        billingService.activate(USER_ID, PlanCode.STARTER);

        verify(subscriptionRepository).save(subscriptionCaptor.capture());
        Subscription subscription = subscriptionCaptor.getValue();
        assertThat(subscription.getPlanCode()).isEqualTo(PlanCode.STARTER);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PENDING_PAYMENT);
        assertThat(subscription.getCurrentPeriodStart()).isNull();
        assertThat(subscription.getCurrentPeriodEnd()).isNull();
    }

    @Test
    void activateRejectsFreeWhenAnotherActiveSubscriptionExists() {
        Subscription active = Subscription.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .planCode(PlanCode.SCALE)
                .status(SubscriptionStatus.ACTIVE)
                .priceCents(10_000)
                .currency("USD")
                .monthlyRequestLimit(15_000)
                .requestsUsed(50)
                .currentPeriodStart(Instant.now().minusSeconds(60))
                .currentPeriodEnd(Instant.now().plusSeconds(60))
                .createdAt(Instant.now().minusSeconds(120))
                .updatedAt(Instant.now().minusSeconds(60))
                .build();
        when(subscriptionRepository.findByUserIdAndStatusIn(any(), any())).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> billingService.activate(USER_ID, PlanCode.FREE))
                .isInstanceOf(SubscriptionAlreadyActiveException.class);
    }

    private static GatewayProperties gatewayProperties() {
        return new GatewayProperties(
                new GatewayProperties.Cors(List.of("http://localhost:5173")),
                new GatewayProperties.Auth("12345678901234567890123456789012", Duration.ofMinutes(15),
                        Duration.ofDays(30), 5, Duration.ofMinutes(15), false),
                new GatewayProperties.Mail("test@example.com", "contact@example.com"),
                new GatewayProperties.Verification("1234567890123456", Duration.ofMinutes(10),
                        Duration.ofSeconds(60), 5),
                new GatewayProperties.Billing(Duration.ofDays(30), List.of(
                        new GatewayProperties.Plan(PlanCode.FREE, "Free", 0, "USD", 10),
                        new GatewayProperties.Plan(PlanCode.STARTER, "Starter", 2_000, "USD", 1_000),
                        new GatewayProperties.Plan(PlanCode.GROWTH, "Growth", 6_000, "USD", 5_000),
                        new GatewayProperties.Plan(PlanCode.SCALE, "Scale", 10_000, "USD", 15_000)
                )),
                new GatewayProperties.ApiKeys("1234567890123456", "rsk_", 5, Duration.ofMinutes(5)),
                new GatewayProperties.PublicScan(new GatewayProperties.RateLimit(10, Duration.ofHours(1))),
                new GatewayProperties.Contact(new GatewayProperties.RateLimit(5, Duration.ofHours(1)))
        );
    }

    private static SubscriptionView mockSubscriptionView() {
        return new SubscriptionView(
                UUID.randomUUID(),
                PlanCode.FREE,
                "Free",
                SubscriptionStatus.ACTIVE,
                0,
                "USD",
                10,
                0,
                10,
                Instant.now(),
                Instant.now().plus(Duration.ofDays(30)),
                Instant.now(),
                null
        );
    }

    private void stubSubscriptionView() {
        when(billingMapper.toView(any())).thenReturn(mockSubscriptionView());
    }
}
