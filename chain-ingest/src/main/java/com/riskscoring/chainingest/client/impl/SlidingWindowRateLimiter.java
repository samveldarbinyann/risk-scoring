package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.RateLimiter;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

public class SlidingWindowRateLimiter implements RateLimiter {

    private static final long WINDOW_NANOS = Duration.ofSeconds(1).toNanos();

    private final int permits;
    private final Deque<Long> issuedAt = new ArrayDeque<>();

    public SlidingWindowRateLimiter(int permits) {
        this.permits = permits;
    }

    @Override
    public synchronized void acquire() {
        long now = System.nanoTime();

        while (issuedAt.size() >= permits) {
            long waitNanos = WINDOW_NANOS - (now - issuedAt.peekFirst());

            if (waitNanos <= 0) {
                issuedAt.pollFirst();
                continue;
            }

            sleep(waitNanos);
            now = System.nanoTime();
        }

        issuedAt.addLast(now);
    }

    private void sleep(long waitNanos) {
        try {
            TimeUnit.NANOSECONDS.sleep(waitNanos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a rate limiter slot", e);
        }
    }
}
