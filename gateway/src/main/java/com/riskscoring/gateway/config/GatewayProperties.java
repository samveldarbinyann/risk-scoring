package com.riskscoring.gateway.config;

import com.riskscoring.gateway.model.PlanCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "gateway")
@Validated
public record GatewayProperties(
        @NotNull @Valid Cors cors,
        @NotNull @Valid Auth auth,
        @NotNull @Valid Mail mail,
        @NotNull @Valid Verification verification,
        @NotNull @Valid Billing billing,
        @NotNull @Valid ApiKeys apiKeys,
        @NotNull @Valid PublicScan publicScan,
        @NotNull @Valid Contact contact,
        @NotNull @Valid PasswordReset passwordReset
) {

    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }

    public record Auth(
            @NotBlank @Size(min = 32) String jwtSecret,
            @NotNull Duration accessTokenTtl,
            @NotNull Duration refreshTokenTtl,
            @Positive int maxFailedLoginAttempts,
            @NotNull Duration lockoutDuration,
            boolean secureCookie
    ) {
    }

    public record Mail(
            @NotBlank @Email String from,
            @NotBlank @Email String contactRecipient
    ) {
    }

    public record Verification(
            @NotBlank @Size(min = 16) String codePepper,
            @NotNull Duration codeTtl,
            @NotNull Duration resendCooldown,
            @Positive int maxAttempts
    ) {
    }

    public record Billing(
            @NotNull Duration period,
            @NotEmpty @Valid List<Plan> plans,
            @NotNull @Valid Payment payment
    ) {
        public Plan requirePlan(PlanCode code) {
            return plans.stream()
                    .filter(plan -> plan.code() == code)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No plan configured for " + code));
        }
    }

    public record Plan(
            @NotNull PlanCode code,
            @PositiveOrZero int priceCents,
            @NotBlank @Size(min = 3, max = 3) String currency,
            @Positive int monthlyRequestLimit
    ) {
    }

    // address left unvalidated on purpose: an unconfigured payment address must not block
    // gateway startup, only the activation of a paid plan (see BillingServiceImpl.applyPaymentRequest).
    public record Payment(
            String address,
            @NotBlank String tokenContractAddress,
            @NotNull Duration window,
            @Positive int tailMinMicroUsdt,
            @Positive int tailMaxMicroUsdt,
            @NotNull Duration reaperFixedDelay
    ) {
    }

    public record ApiKeys(
            @NotBlank @Size(min = 16) String pepper,
            @NotBlank String prefix,
            @Positive int maxPerUser,
            @NotNull Duration lastUsedThrottle
    ) {
    }

    public record PublicScan(
            @NotNull @Valid RateLimit rateLimit,
            @Positive int maxChains
    ) {
    }

    public record Contact(@NotNull @Valid RateLimit rateLimit) {
    }

    public record PasswordReset(@NotNull @Valid RateLimit rateLimit) {
    }

    public record RateLimit(
            @Positive int requests,
            @NotNull Duration window
    ) {
    }
}
