package com.riskscoring.paymentwatch.config;

import com.riskscoring.common.model.Chain;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "payment-watch")
@Validated
public record PaymentWatchProperties(
        // Left unvalidated on purpose: an unconfigured address must not block service startup,
        // only make pollForPayments() skip its work (see PaymentWatchServiceImpl).
        String targetAddress,
        @NotNull Chain chain,
        @NotBlank String usdtContractAddress,
        @NotNull Duration pollInterval,
        @NotNull Duration confirmationWindow,
        @NotNull Duration lookbackWindow,
        @NotNull @Valid Moralis moralis
) {

    public record Moralis(
            @NotBlank String baseUrl,
            @NotBlank String apiKey,
            @Positive int callsPerSecond,
            @Positive int rateLimitRetries,
            @NotNull Duration rateLimitBackoff,
            @NotNull Duration connectTimeout,
            @NotNull Duration readTimeout
    ) {
    }
}
