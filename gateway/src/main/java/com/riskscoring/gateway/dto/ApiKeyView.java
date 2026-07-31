package com.riskscoring.gateway.dto;

import com.riskscoring.gateway.model.ApiKeyStatus;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyView(
        UUID id,
        String name,
        String keyPrefix,
        ApiKeyStatus status,
        Instant lastUsedAt,
        Instant createdAt,
        Instant revokedAt
) {
}
