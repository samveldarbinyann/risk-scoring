package com.riskscoring.gateway.model;

import java.time.Duration;
import java.time.Instant;

public final class BillingPeriods {

    private BillingPeriods() {
    }

    public static Instant startOfPeriodContaining(Instant anchor, Duration period, Instant now) {
        long elapsedPeriods = Duration.between(anchor, now).dividedBy(period);
        return elapsedPeriods <= 0 ? anchor : anchor.plus(period.multipliedBy(elapsedPeriods));
    }
}
