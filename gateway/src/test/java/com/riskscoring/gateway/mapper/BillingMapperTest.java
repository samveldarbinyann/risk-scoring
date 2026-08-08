package com.riskscoring.gateway.mapper;

import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.dto.PlanView;
import com.riskscoring.gateway.dto.SubscriptionView;
import com.riskscoring.gateway.entity.Subscription;
import com.riskscoring.gateway.model.PlanCode;
import com.riskscoring.gateway.model.SubscriptionStatus;
import com.riskscoring.gateway.support.GatewayPropertiesFixture;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BillingMapperTest {

    private static final Duration PERIOD = Duration.ofDays(30);
    private static final int MONTHLY_LIMIT = 10;

    private final BillingMapper mapper = new BillingMapper(GatewayPropertiesFixture.builder()
            .billingPeriod(PERIOD)
            .plans(GatewayPropertiesFixture.FREE_PLAN, GatewayPropertiesFixture.STARTER_PLAN)
            .build());

    @Test
    void toPlanViewMapsAllFields() {
        GatewayProperties.Plan plan = new GatewayProperties.Plan(PlanCode.STARTER, 2_000, "USD", 1_000);

        PlanView view = mapper.toPlanView(plan);

        assertThat(view).isEqualTo(new PlanView(PlanCode.STARTER, 2_000, "USD", 1_000));
    }

    @Test
    void toViewPassesThroughValuesForActiveSubscriptionWithinPeriod() {
        Instant currentPeriodStart = Instant.now().minus(Duration.ofDays(5));
        Instant currentPeriodEnd = currentPeriodStart.plus(PERIOD);
        Subscription subscription = subscription(SubscriptionStatus.ACTIVE, currentPeriodStart, currentPeriodEnd, 7);

        SubscriptionView view = mapper.toView(subscription);

        assertThat(view.currentPeriodStart()).isEqualTo(currentPeriodStart);
        assertThat(view.currentPeriodEnd()).isEqualTo(currentPeriodEnd);
        assertThat(view.requestsUsed()).isEqualTo(7);
        assertThat(view.requestsRemaining()).isEqualTo(3);
    }

    @Test
    void toViewVirtuallyRollsPeriodForActiveSubscriptionPastPeriodEnd() {
        Instant currentPeriodStart = Instant.now().minus(Duration.ofDays(65));
        Instant currentPeriodEnd = currentPeriodStart.plus(PERIOD);
        Subscription subscription = subscription(SubscriptionStatus.ACTIVE, currentPeriodStart, currentPeriodEnd, 7);

        SubscriptionView view = mapper.toView(subscription);

        Instant expectedStart = currentPeriodStart.plus(PERIOD.multipliedBy(2));
        assertThat(view.currentPeriodStart()).isEqualTo(expectedStart);
        assertThat(view.currentPeriodEnd()).isEqualTo(expectedStart.plus(PERIOD));
        assertThat(view.requestsUsed()).isZero();
        assertThat(view.requestsRemaining()).isEqualTo(MONTHLY_LIMIT);
    }

    @Test
    void toViewDoesNotRollNonActiveSubscriptionEvenWithOldDates() {
        Instant currentPeriodStart = Instant.now().minus(Duration.ofDays(65));
        Instant currentPeriodEnd = currentPeriodStart.plus(PERIOD);
        Subscription subscription = subscription(SubscriptionStatus.PENDING_PAYMENT, currentPeriodStart, currentPeriodEnd, 3);

        SubscriptionView view = mapper.toView(subscription);

        assertThat(view.currentPeriodStart()).isEqualTo(currentPeriodStart);
        assertThat(view.currentPeriodEnd()).isEqualTo(currentPeriodEnd);
        assertThat(view.requestsUsed()).isEqualTo(3);
    }

    @Test
    void toViewClampsRequestsRemainingToZeroWhenUsedExceedsLimit() {
        Instant currentPeriodStart = Instant.now().minus(Duration.ofDays(5));
        Instant currentPeriodEnd = currentPeriodStart.plus(PERIOD);
        Subscription subscription = subscription(SubscriptionStatus.ACTIVE, currentPeriodStart, currentPeriodEnd, 999);

        SubscriptionView view = mapper.toView(subscription);

        assertThat(view.requestsRemaining()).isZero();
    }

    @Test
    void toViewBuildsAnEip681PaymentUriWhenPaymentFieldsAreSet() {
        Subscription subscription = subscription(SubscriptionStatus.PENDING_PAYMENT, null, null, 0);
        subscription.setPaymentAddress("0xTestRecipient");
        subscription.setPaymentAmount(new BigDecimal("1.004137"));

        SubscriptionView view = mapper.toView(subscription);

        assertThat(view.paymentUri()).isEqualTo(
                "ethereum:0xTestUsdtContract@56/transfer?address=0xTestRecipient&uint256=1004137000000000000");
    }

    @Test
    void toViewLeavesPaymentUriNullWhenSubscriptionHasNoPendingPayment() {
        Subscription subscription = subscription(SubscriptionStatus.ACTIVE, Instant.now(), Instant.now().plus(PERIOD), 0);

        SubscriptionView view = mapper.toView(subscription);

        assertThat(view.paymentUri()).isNull();
    }

    private static Subscription subscription(SubscriptionStatus status, Instant periodStart, Instant periodEnd, int requestsUsed) {
        return Subscription.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .planCode(PlanCode.STARTER)
                .status(status)
                .priceCents(2_000)
                .currency("USD")
                .monthlyRequestLimit(MONTHLY_LIMIT)
                .requestsUsed(requestsUsed)
                .currentPeriodStart(periodStart)
                .currentPeriodEnd(periodEnd)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

}
