package com.riskscoring.gateway.entity;

import com.riskscoring.gateway.model.PlanCode;
import com.riskscoring.gateway.model.SubscriptionStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionTest {

    @Test
    void instancesWithSameNonNullIdAreEqual() {
        UUID id = UUID.randomUUID();
        Subscription a = subscription(id);
        Subscription b = subscription(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void instancesWithDifferentIdsAreNotEqual() {
        Subscription a = subscription(UUID.randomUUID());
        Subscription b = subscription(UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void instancesWithNullIdAreNeverEqualToEachOther() {
        Subscription a = subscription(null);
        Subscription b = subscription(null);

        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void isEqualToItself() {
        Subscription a = subscription(UUID.randomUUID());

        assertThat(a).isEqualTo(a);
    }

    @Test
    void isNotEqualToNullOrUnrelatedType() {
        Subscription a = subscription(UUID.randomUUID());

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not a Subscription");
    }

    private static Subscription subscription(UUID id) {
        Instant now = Instant.now();
        return Subscription.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .planCode(PlanCode.FREE)
                .status(SubscriptionStatus.ACTIVE)
                .priceCents(0)
                .currency("USD")
                .monthlyRequestLimit(10)
                .requestsUsed(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
