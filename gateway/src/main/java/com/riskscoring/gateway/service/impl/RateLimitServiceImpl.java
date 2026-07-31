package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.exception.RateLimitExceededException;
import com.riskscoring.gateway.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private static final int EVICTION_THRESHOLD = 10_000;
    private static final String PUBLIC_SCAN_BUCKET = "public-scan";
    private static final String CONTACT_BUCKET = "contact";

    private final GatewayProperties gatewayProperties;
    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public void checkPublicScan(String clientIp) {
        check(PUBLIC_SCAN_BUCKET, clientIp, gatewayProperties.publicScan().rateLimit());
    }

    @Override
    public void checkContact(String clientIp) {
        check(CONTACT_BUCKET, clientIp, gatewayProperties.contact().rateLimit());
    }

    private void check(String bucket, String clientIp, GatewayProperties.RateLimit rateLimit) {
        Instant now = Instant.now();
        evictExpired(now);

        Window window = windows.compute(bucket + ":" + clientIp, (key, current) ->
                current == null || current.isExpired(now)
                        ? new Window(now.plus(rateLimit.window()), 1)
                        : current.increment());

        if (window.hits() > rateLimit.requests()) {
            throw new RateLimitExceededException(Duration.between(now, window.expiresAt()).toSeconds());
        }
    }

    private void evictExpired(Instant now) {
        if (windows.size() > EVICTION_THRESHOLD) {
            windows.values().removeIf(window -> window.isExpired(now));
        }
    }

    private record Window(Instant expiresAt, int hits) {

        private boolean isExpired(Instant now) {
            return !now.isBefore(expiresAt);
        }

        private Window increment() {
            return new Window(expiresAt, hits + 1);
        }
    }
}
