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
import com.riskscoring.gateway.security.ApiKeyHasher;
import com.riskscoring.gateway.security.ApiKeyPrincipal;
import com.riskscoring.gateway.service.ApiKeyService;
import com.riskscoring.gateway.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl implements ApiKeyService {

    private static final int KEY_BYTES = 32;
    private static final int PREFIX_LENGTH = 12;
    private static final Base64.Encoder KEY_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyMapper apiKeyMapper;
    private final ApiKeyHasher apiKeyHasher;
    private final BillingService billingService;
    private final GatewayProperties gatewayProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public ApiKeyCreatedView create(UUID userId, CreateApiKeyRequest request) {
        billingService.requireActiveSubscription(userId);

        int maxKeys = gatewayProperties.billing().maxApiKeysPerUser();
        long activeCount = apiKeyRepository.countByUserIdAndStatus(userId, ApiKeyStatus.ACTIVE);
        if (activeCount >= maxKeys) {
            throw new ApiKeyLimitExceededException(maxKeys);
        }

        String plaintext = generatePlaintext();
        Instant now = Instant.now();
        ApiKey apiKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name(request.name().trim())
                .keyPrefix(plaintext.substring(0, Math.min(PREFIX_LENGTH, plaintext.length())))
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

        Instant now = Instant.now();
        apiKey.setStatus(ApiKeyStatus.REVOKED);
        apiKey.setRevokedAt(now);
        apiKeyRepository.save(apiKey);
    }

    @Override
    @Transactional
    public Optional<ApiKeyPrincipal> resolveActiveKey(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            return Optional.empty();
        }

        String hash = apiKeyHasher.hash(rawApiKey.trim());
        return apiKeyRepository.findByKeyHashAndStatus(hash, ApiKeyStatus.ACTIVE)
                .map(key -> {
                    apiKeyRepository.touchLastUsedAt(key.getId(), Instant.now());
                    return new ApiKeyPrincipal(key.getUserId(), key.getId());
                });
    }

    private String generatePlaintext() {
        byte[] bytes = new byte[KEY_BYTES];
        secureRandom.nextBytes(bytes);
        return gatewayProperties.apiKeys().prefix() + KEY_ENCODER.encodeToString(bytes);
    }
}
