package com.riskscoring.chainingest.client.impl;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SlidingWindowRateLimiterTest {

    @Test
    void acquireDoesNotBlockMeasurablyWhileUnderThePermitBudget() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(5);

        Instant start = Instant.now();
        for (int i = 0; i < 5; i++) {
            limiter.acquire();
        }
        Duration elapsed = Duration.between(start, Instant.now());

        assertThat(elapsed).isLessThan(Duration.ofMillis(500));
    }

    @Test
    void acquireBlocksOnceThePermitBudgetIsExceeded() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(1);

        Instant start = Instant.now();
        limiter.acquire();
        limiter.acquire();
        Duration elapsed = Duration.between(start, Instant.now());

        assertThat(elapsed).isGreaterThan(Duration.ofMillis(50));
    }
}
