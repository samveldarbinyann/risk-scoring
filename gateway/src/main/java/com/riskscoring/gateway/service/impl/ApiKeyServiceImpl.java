package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.dto.ApiKeyCreatedView;
import com.riskscoring.gateway.dto.ApiKeyView;
import com.riskscoring.gateway.dto.CreateApiKeyRequest;
import com.riskscoring.gateway.entity.ApiKey;
import com.riskscoring.gateway.exception.ApiKeyLimitExceededException;
import com.riskscoring.gateway.exception.ApiKeyNotFoundException;
import com.riskscoring.gateway.mapper.ApiKeyMapper;
import com.riskscoring.gateway.model.ApiKeyStatus;
import com.riskscoring.gateway.repository.ApiKeyRepository;
import com.riskscoring.gateway.security.ApiKeyPrincipal;
import com.riskscoring.gateway.security.SecretGenerator;
import com.riskscoring.gateway.security.SecretHasher;
import com.riskscoring.gateway.service.ApiKeyService;
import com.riskscoring.gateway.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl implements ApiKeyService {

    private static final int PUBLIC_ID_BYTES = 6;
    private static final String SEGMENT_SEPARATOR = "_";

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyMapper apiKeyMapper;
    private final SecretHasher apiKeyHasher;
    private final SecretGenerator secretGenerator;
    private final BillingService billingService;
    private final GatewayProperties gatewayProperties;

    @Override
    @Transactional
    public ApiKeyCreatedView create(UUID userId, CreateApiKeyRequest request) {
        billingService.requireActiveSubscription(userId);

        int maxKeys = gatewayProperties.apiKeys().maxPerUser();
        long activeCount = apiKeyRepository.countByUserIdAndStatus(userId, ApiKeyStatus.ACTIVE);
        if (activeCount >= maxKeys) {
            throw new ApiKeyLimitExceededException(maxKeys);
        }

        String keyPrefix = gatewayProperties.apiKeys().prefix() + secretGenerator.generate(PUBLIC_ID_BYTES);
        String plaintext = keyPrefix + SEGMENT_SEPARATOR + secretGenerator.generate();
        Instant now = Instant.now();
        ApiKey apiKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name(request.name().trim())
                .keyPrefix(keyPrefix)
                .keyHash(apiKeyHasher.hash(plaintext))
                .status(ApiKeyStatus.ACTIVE)
                .createdAt(now)
                .build();

        return apiKeyMapper.toCreatedView(apiKeyRepository.save(apiKey), plaintext);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKeyView> list(UUID userId) {
        return apiKeyRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(apiKeyMapper::toView)
                .toList();
    }

    @Override
    @Transactional
    public void revoke(UUID userId, UUID keyId) {
        ApiKey apiKey = apiKeyRepository.findByIdAndUserId(keyId, userId)
                .orElseThrow(() -> new ApiKeyNotFoundException(keyId));

        if (apiKey.getStatus() == ApiKeyStatus.REVOKED) {
            return;
        }

        apiKey.setStatus(ApiKeyStatus.REVOKED);
        apiKey.setRevokedAt(Instant.now());
    }

    @Override
    @Transactional
    public void revokeAllActive(UUID userId) {
        apiKeyRepository.revokeAllActive(
                userId, Instant.now(), ApiKeyStatus.ACTIVE, ApiKeyStatus.REVOKED);
    }

    @Override
    @Transactional
    public Optional<ApiKeyPrincipal> resolveActiveKey(String rawApiKey) {
        return apiKeyRepository.findByKeyHashAndStatus(apiKeyHasher.hash(rawApiKey), ApiKeyStatus.ACTIVE)
                .map(key -> {
                    ApiKeyPrincipal principal = new ApiKeyPrincipal(key.getUserId(), key.getId());
                    Instant now = Instant.now();
                    apiKeyRepository.touchLastUsedAt(
                            principal.apiKeyId(), now, now.minus(gatewayProperties.apiKeys().lastUsedThrottle()));
                    return principal;
                });
    }
}
