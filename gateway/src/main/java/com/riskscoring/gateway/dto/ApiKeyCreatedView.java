package com.riskscoring.gateway.dto;

import com.riskscoring.gateway.model.ApiKeyStatus;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyCreatedView(
        UUID id,
        String name,
        String keyPrefix,
        String apiKey,
        ApiKeyStatus status,
        Instant createdAt
) {
}
