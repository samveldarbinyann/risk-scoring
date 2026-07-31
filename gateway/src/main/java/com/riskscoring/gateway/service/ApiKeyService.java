package com.riskscoring.gateway.service;

import com.riskscoring.gateway.dto.ApiKeyCreatedView;
import com.riskscoring.gateway.dto.ApiKeyView;
import com.riskscoring.gateway.dto.CreateApiKeyRequest;
import com.riskscoring.gateway.security.ApiKeyPrincipal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyService {

    ApiKeyCreatedView create(UUID userId, CreateApiKeyRequest request);

    List<ApiKeyView> list(UUID userId);

    void revoke(UUID userId, UUID keyId);

    void revokeAllActive(UUID userId);

    Optional<ApiKeyPrincipal> resolveActiveKey(String rawApiKey);
}
