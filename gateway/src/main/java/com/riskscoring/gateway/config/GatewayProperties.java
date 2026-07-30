package com.riskscoring.gateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
        @NotNull @Valid Verification verification
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

    public record Mail(@NotBlank @Email String from) {
    }

    public record Verification(
            @NotBlank @Size(min = 16) String codePepper,
            @NotNull Duration codeTtl,
            @NotNull Duration resendCooldown,
            @Positive int maxAttempts
    ) {
    }
}
