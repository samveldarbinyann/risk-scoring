package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.dto.PlanView;
import com.riskscoring.gateway.dto.SubscriptionView;
import com.riskscoring.gateway.entity.Subscription;
import com.riskscoring.gateway.exception.NoActiveSubscriptionException;
import com.riskscoring.gateway.exception.QuotaExceededException;
import com.riskscoring.gateway.exception.SubscriptionAlreadyActiveException;
import com.riskscoring.gateway.exception.SubscriptionNotFoundException;
import com.riskscoring.gateway.exception.SubscriptionNotPendingException;
import com.riskscoring.gateway.mapper.BillingMapper;
import com.riskscoring.gateway.model.PlanCode;
import com.riskscoring.gateway.model.SubscriptionStatus;
import com.riskscoring.gateway.repository.SubscriptionRepository;
import com.riskscoring.gateway.service.ApiKeyService;
import com.riskscoring.gateway.support.GatewayPropertiesFixture;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Mock
    private ApiKeyService apiKeyServiceMock;

    @Captor
    private ArgumentCaptor<Subscription> subscriptionCaptor;

    private BillingServiceImpl billingService;

    @BeforeEach
    void setUp() {
        billingService = new BillingServiceImpl(subscriptionRepository, billingMapper, allPlans(), apiKeyService);
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

    @Test
    void activateRepricesPendingSubscriptionToADifferentPaidPlanWithoutTouchingPeriodDates() {
        Subscription pending = Subscription.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .planCode(PlanCode.STARTER)
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .priceCents(2_000)
                .currency("USD")
                .monthlyRequestLimit(1_000)
                .requestsUsed(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(subscriptionRepository.findByUserIdAndStatusIn(any(), any())).thenReturn(Optional.of(pending));
        stubSubscriptionView();

        billingService.activate(USER_ID, PlanCode.GROWTH);

        assertThat(pending.getPlanCode()).isEqualTo(PlanCode.GROWTH);
        assertThat(pending.getStatus()).isEqualTo(SubscriptionStatus.PENDING_PAYMENT);
        assertThat(pending.getPriceCents()).isEqualTo(6_000);
        assertThat(pending.getMonthlyRequestLimit()).isEqualTo(5_000);
        assertThat(pending.getCurrentPeriodStart()).isNull();
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void activateThrowsSubscriptionAlreadyActiveExceptionWhenRequestingAPaidPlanAndAlreadyActive() {
        Subscription active = Subscription.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .planCode(PlanCode.STARTER)
                .status(SubscriptionStatus.ACTIVE)
                .priceCents(2_000)
                .currency("USD")
                .monthlyRequestLimit(1_000)
                .requestsUsed(0)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(Instant.now().plusSeconds(60))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(subscriptionRepository.findByUserIdAndStatusIn(any(), any())).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> billingService.activate(USER_ID, PlanCode.GROWTH))
                .isInstanceOf(SubscriptionAlreadyActiveException.class);
    }

    @Test
    void listPlansMapsEveryConfiguredPlan() {
        when(billingMapper.toPlanView(any())).thenAnswer(invocation -> {
            GatewayProperties.Plan plan = invocation.getArgument(0);
            return new PlanView(plan.code(), plan.priceCents(), plan.currency(), plan.monthlyRequestLimit());
        });

        List<PlanView> views = billingService.listPlans();

        assertThat(views).extracting(PlanView::code)
                .containsExactly(PlanCode.FREE, PlanCode.STARTER, PlanCode.GROWTH, PlanCode.SCALE);
    }

    @Test
    void getSubscriptionReturnsLiveSubscriptionWhenFound() {
        Subscription live = liveSubscription();
        when(subscriptionRepository.findByUserIdAndStatusIn(any(), any())).thenReturn(Optional.of(live));
        stubSubscriptionView();

        SubscriptionView view = billingService.getSubscription(USER_ID);

        assertThat(view).isNotNull();
        verify(subscriptionRepository, never()).findFirstByUserIdOrderByCreatedAtDesc(any());
    }

    @Test
    void getSubscriptionFallsBackToMostRecentWhenNoLiveSubscriptionExists() {
        Subscription past = liveSubscription();
        when(subscriptionRepository.findByUserIdAndStatusIn(any(), any())).thenReturn(Optional.empty());
        when(subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(Optional.of(past));
        stubSubscriptionView();

        billingService.getSubscription(USER_ID);

        verify(subscriptionRepository).findFirstByUserIdOrderByCreatedAtDesc(USER_ID);
    }

    @Test
    void getSubscriptionThrowsSubscriptionNotFoundExceptionWhenNoneExistAtAll() {
        when(subscriptionRepository.findByUserIdAndStatusIn(any(), any())).thenReturn(Optional.empty());
        when(subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billingService.getSubscription(USER_ID))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void confirmPaymentThrowsSubscriptionNotFoundExceptionWhenNotFound() {
        UUID subscriptionId = UUID.randomUUID();
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billingService.confirmPayment(USER_ID, subscriptionId))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void confirmPaymentThrowsSubscriptionNotFoundExceptionWhenOwnedByAnotherUser() {
        Subscription pending = pendingSubscription(UUID.randomUUID());
        when(subscriptionRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> billingService.confirmPayment(USER_ID, pending.getId()))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void confirmPaymentThrowsSubscriptionNotPendingExceptionWhenAlreadyActive() {
        Subscription active = liveSubscription();
        when(subscriptionRepository.findById(active.getId())).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> billingService.confirmPayment(USER_ID, active.getId()))
                .isInstanceOf(SubscriptionNotPendingException.class);
    }

    @Test
    void confirmPaymentActivatesPendingSubscriptionAndResetsUsage() {
        Subscription pending = pendingSubscription(USER_ID);
        pending.setRequestsUsed(7);
        when(subscriptionRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        stubSubscriptionView();

        billingService.confirmPayment(USER_ID, pending.getId());

        assertThat(pending.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(pending.getRequestsUsed()).isZero();
        assertThat(pending.getCurrentPeriodStart()).isNotNull();
        assertThat(pending.getCurrentPeriodEnd()).isEqualTo(pending.getCurrentPeriodStart().plus(Duration.ofDays(30)));
    }

    @Test
    void cancelThrowsNoActiveSubscriptionExceptionWhenNoneLive() {
        when(subscriptionRepository.findByUserIdAndStatusIn(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billingService.cancel(USER_ID))
                .isInstanceOf(NoActiveSubscriptionException.class);

        verifyNoInteractions(apiKeyService);
    }

    @Test
    void cancelMarksSubscriptionCanceledAndRevokesActiveApiKeys() {
        Subscription live = liveSubscription();
        when(subscriptionRepository.findByUserIdAndStatusIn(any(), any())).thenReturn(Optional.of(live));
        when(apiKeyService.getObject()).thenReturn(apiKeyServiceMock);
        stubSubscriptionView();

        billingService.cancel(USER_ID);

        assertThat(live.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(live.getCanceledAt()).isNotNull();
        verify(apiKeyServiceMock).revokeAllActive(USER_ID);
    }

    @Test
    void requireActiveSubscriptionDoesNothingWhenActiveSubscriptionExists() {
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(liveSubscription()));

        billingService.requireActiveSubscription(USER_ID);
    }

    @Test
    void requireActiveSubscriptionThrowsWhenNoneActive() {
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billingService.requireActiveSubscription(USER_ID))
                .isInstanceOf(NoActiveSubscriptionException.class);
    }

    @Test
    void chargeQuotaThrowsIllegalArgumentExceptionForNonPositiveUnits() {
        assertThatThrownBy(() -> billingService.chargeQuota(USER_ID, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> billingService.chargeQuota(USER_ID, -1)).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void chargeQuotaThrowsNoActiveSubscriptionExceptionWhenNoneActive() {
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billingService.chargeQuota(USER_ID, 1))
                .isInstanceOf(NoActiveSubscriptionException.class);
    }

    @Test
    void chargeQuotaSucceedsWithoutRollingPeriodWhenWithinCurrentPeriod() {
        Subscription active = liveSubscription();
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE)).thenReturn(Optional.of(active));
        when(subscriptionRepository.tryCharge(eq(active.getId()), eq(1), any(), eq(SubscriptionStatus.ACTIVE))).thenReturn(1);

        billingService.chargeQuota(USER_ID, 1);

        verify(subscriptionRepository, never()).rollPeriod(any(), any(), any(), any(), any());
    }

    @Test
    void chargeQuotaRollsPeriodWhenCurrentPeriodHasEnded() {
        Subscription active = Subscription.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .planCode(PlanCode.STARTER)
                .status(SubscriptionStatus.ACTIVE)
                .priceCents(2_000)
                .currency("USD")
                .monthlyRequestLimit(1_000)
                .requestsUsed(500)
                .currentPeriodStart(Instant.now().minusSeconds(120))
                .currentPeriodEnd(Instant.now().minusSeconds(60))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(active), Optional.empty());
        when(subscriptionRepository.findById(active.getId())).thenReturn(Optional.of(active));
        when(subscriptionRepository.tryCharge(eq(active.getId()), eq(1), any(), eq(SubscriptionStatus.ACTIVE))).thenReturn(1);

        billingService.chargeQuota(USER_ID, 1);

        verify(subscriptionRepository).rollPeriod(eq(active.getId()), any(), any(), any(), eq(SubscriptionStatus.ACTIVE));
    }

    @Test
    void chargeQuotaThrowsQuotaExceededExceptionWhenTryChargeFailsButSubscriptionStillActive() {
        Subscription active = liveSubscription();
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE)).thenReturn(Optional.of(active));
        when(subscriptionRepository.tryCharge(eq(active.getId()), eq(1), any(), eq(SubscriptionStatus.ACTIVE))).thenReturn(0);
        when(subscriptionRepository.findById(active.getId())).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> billingService.chargeQuota(USER_ID, 1))
                .isInstanceOf(QuotaExceededException.class);
    }

    @Test
    void chargeQuotaThrowsNoActiveSubscriptionExceptionWhenTryChargeFailsBecauseSubscriptionWasCanceled() {
        Subscription active = liveSubscription();
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE)).thenReturn(Optional.of(active));
        when(subscriptionRepository.tryCharge(eq(active.getId()), eq(1), any(), eq(SubscriptionStatus.ACTIVE))).thenReturn(0);
        Subscription canceled = liveSubscription();
        canceled.setStatus(SubscriptionStatus.CANCELED);
        when(subscriptionRepository.findById(active.getId())).thenReturn(Optional.of(canceled));

        assertThatThrownBy(() -> billingService.chargeQuota(USER_ID, 1))
                .isInstanceOf(NoActiveSubscriptionException.class);
    }

    private static Subscription liveSubscription() {
        return Subscription.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .planCode(PlanCode.STARTER)
                .status(SubscriptionStatus.ACTIVE)
                .priceCents(2_000)
                .currency("USD")
                .monthlyRequestLimit(1_000)
                .requestsUsed(10)
                .currentPeriodStart(Instant.now().minusSeconds(60))
                .currentPeriodEnd(Instant.now().plusSeconds(600))
                .createdAt(Instant.now().minusSeconds(120))
                .updatedAt(Instant.now().minusSeconds(60))
                .build();
    }

    private static Subscription pendingSubscription(UUID userId) {
        return Subscription.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .planCode(PlanCode.STARTER)
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .priceCents(2_000)
                .currency("USD")
                .monthlyRequestLimit(1_000)
                .requestsUsed(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }


    private static SubscriptionView mockSubscriptionView() {
        return new SubscriptionView(
                UUID.randomUUID(),
                PlanCode.FREE,
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

    private static GatewayProperties allPlans() {
        return GatewayPropertiesFixture.builder()
                .plans(GatewayPropertiesFixture.FREE_PLAN, GatewayPropertiesFixture.STARTER_PLAN,
                        GatewayPropertiesFixture.GROWTH_PLAN, GatewayPropertiesFixture.SCALE_PLAN)
                .build();
    }
}
