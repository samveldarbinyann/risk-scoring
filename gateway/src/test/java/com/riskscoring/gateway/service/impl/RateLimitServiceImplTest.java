package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.exception.RateLimitExceededException;
import com.riskscoring.gateway.support.GatewayPropertiesFixture;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitServiceImplTest {

    @Test
    void checkPublicScanAllowsRequestsWithinLimit() {
        RateLimitServiceImpl service = new RateLimitServiceImpl(
                gatewayProperties(new GatewayProperties.RateLimit(2, Duration.ofMinutes(1))));

        assertThatCode(() -> {
            service.checkPublicScan("203.0.113.10");
            service.checkPublicScan("203.0.113.10");
        }).doesNotThrowAnyException();
    }

    @Test
    void checkPublicScanThrowsWhenLimitExceeded() {
        RateLimitServiceImpl service = new RateLimitServiceImpl(
                gatewayProperties(new GatewayProperties.RateLimit(1, Duration.ofMinutes(1))));
        service.checkPublicScan("203.0.113.10");

        assertThatThrownBy(() -> service.checkPublicScan("203.0.113.10"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void checkPublicScanIsolatesDifferentClientIps() {
        RateLimitServiceImpl service = new RateLimitServiceImpl(
                gatewayProperties(new GatewayProperties.RateLimit(1, Duration.ofMinutes(1))));
        service.checkPublicScan("203.0.113.10");

        assertThatCode(() -> service.checkPublicScan("203.0.113.11")).doesNotThrowAnyException();
    }

    @Test
    void checkContactAndCheckPublicScanUseIsolatedBucketsForTheSameIp() {
        GatewayProperties.RateLimit tight = new GatewayProperties.RateLimit(1, Duration.ofMinutes(1));
        RateLimitServiceImpl service = new RateLimitServiceImpl(gatewayPropertiesWithAllLimits(tight));
        service.checkPublicScan("203.0.113.10");

        assertThatCode(() -> service.checkContact("203.0.113.10")).doesNotThrowAnyException();
    }

    @Test
    void checkPublicScanAllowsRequestsAgainAfterWindowExpires() throws InterruptedException {
        RateLimitServiceImpl service = new RateLimitServiceImpl(
                gatewayProperties(new GatewayProperties.RateLimit(1, Duration.ofMillis(50))));
        service.checkPublicScan("203.0.113.10");
        assertThatThrownBy(() -> service.checkPublicScan("203.0.113.10"))
                .isInstanceOf(RateLimitExceededException.class);

        Thread.sleep(80);

        assertThatCode(() -> service.checkPublicScan("203.0.113.10")).doesNotThrowAnyException();
    }

    private static GatewayProperties gatewayProperties(GatewayProperties.RateLimit publicScanLimit) {
        GatewayProperties.RateLimit relaxed = new GatewayProperties.RateLimit(1000, Duration.ofMinutes(1));
        return GatewayPropertiesFixture.builder()
                .publicScanLimit(publicScanLimit)
                .contactLimit(relaxed)
                .passwordResetLimit(relaxed)
                .build();
    }

    private static GatewayProperties gatewayPropertiesWithAllLimits(GatewayProperties.RateLimit limit) {
        return GatewayPropertiesFixture.builder()
                .publicScanLimit(limit)
                .contactLimit(limit)
                .passwordResetLimit(limit)
                .build();
    }
}
