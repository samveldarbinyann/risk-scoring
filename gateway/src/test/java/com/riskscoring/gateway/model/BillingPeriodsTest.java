package com.riskscoring.gateway.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BillingPeriodsTest {

    private static final Instant ANCHOR = Instant.parse("2024-01-01T00:00:00Z");
    private static final Duration PERIOD = Duration.ofDays(30);

    @Test
    void returnsAnchorWhenNowEqualsAnchor() {
        assertThat(BillingPeriods.startOfPeriodContaining(ANCHOR, PERIOD, ANCHOR)).isEqualTo(ANCHOR);
    }

    @Test
    void returnsAnchorWhenLessThanOnePeriodHasElapsed() {
        Instant now = ANCHOR.plus(Duration.ofDays(10));

        assertThat(BillingPeriods.startOfPeriodContaining(ANCHOR, PERIOD, now)).isEqualTo(ANCHOR);
    }

    @Test
    void returnsAnchorWhenNowIsBeforeAnchor() {
        Instant now = ANCHOR.minus(Duration.ofDays(5));

        assertThat(BillingPeriods.startOfPeriodContaining(ANCHOR, PERIOD, now)).isEqualTo(ANCHOR);
    }

    @Test
    void returnsAnchorPlusWholePeriodsWhenExactlyOnBoundary() {
        Instant now = ANCHOR.plus(Duration.ofDays(60));

        assertThat(BillingPeriods.startOfPeriodContaining(ANCHOR, PERIOD, now))
                .isEqualTo(ANCHOR.plus(Duration.ofDays(60)));
    }

    @Test
    void truncatesPartialPeriodsInsteadOfRounding() {
        Instant now = ANCHOR.plus(Duration.ofDays(65));

        assertThat(BillingPeriods.startOfPeriodContaining(ANCHOR, PERIOD, now))
                .isEqualTo(ANCHOR.plus(Duration.ofDays(60)));
    }
}
