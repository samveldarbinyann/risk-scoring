package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.exception.RateLimitExceededException;
import com.riskscoring.gateway.model.PlanCode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

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
        return build(publicScanLimit, relaxed, relaxed);
    }

    private static GatewayProperties gatewayPropertiesWithAllLimits(GatewayProperties.RateLimit limit) {
        return build(limit, limit, limit);
    }

    private static GatewayProperties build(GatewayProperties.RateLimit publicScanLimit,
                                           GatewayProperties.RateLimit contactLimit,
                                           GatewayProperties.RateLimit passwordResetLimit) {
        return new GatewayProperties(
                new GatewayProperties.Cors(List.of("http://localhost:5173")),
                new GatewayProperties.Auth("12345678901234567890123456789012", Duration.ofMinutes(15),
                        Duration.ofDays(30), 5, Duration.ofMinutes(15), false),
                new GatewayProperties.Mail("test@example.com", "contact@example.com"),
                new GatewayProperties.Verification("1234567890123456", Duration.ofMinutes(10),
                        Duration.ofSeconds(60), 5),
                new GatewayProperties.Billing(Duration.ofDays(30), List.of(
                        new GatewayProperties.Plan(PlanCode.FREE, 0, "USD", 10))),
                new GatewayProperties.ApiKeys("1234567890123456", "rsk_", 5, Duration.ofMinutes(5)),
                new GatewayProperties.PublicScan(publicScanLimit),
                new GatewayProperties.Contact(contactLimit),
                new GatewayProperties.PasswordReset(passwordResetLimit)
        );
    }
}
