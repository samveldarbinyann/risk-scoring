package com.riskscoring.gateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@ConfigurationProperties(prefix = "gateway")
@Validated
public record GatewayProperties(@NotNull @Valid Cors cors) {

    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }
}
