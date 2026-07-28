package com.riskscoring.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties(Cors cors) {

    public record Cors(List<String> allowedOrigins) {
    }
}
