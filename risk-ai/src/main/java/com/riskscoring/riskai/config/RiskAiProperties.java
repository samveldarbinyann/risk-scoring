package com.riskscoring.riskai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "risk-ai")
public record RiskAiProperties(
        Llm llm,
        int maxVerdictAttempts,
        String promptVersion
) {
    public record Llm(
            String baseUrl,
            String apiKey,
            String model,
            double temperature,
            Duration connectTimeout,
            Duration readTimeout
    ) {
    }
}
