package com.riskscoring.gateway.security;

import java.util.UUID;

public record ApiKeyPrincipal(UUID userId, UUID apiKeyId) {
}
