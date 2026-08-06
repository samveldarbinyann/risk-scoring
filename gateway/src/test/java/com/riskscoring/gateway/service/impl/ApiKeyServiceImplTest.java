package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.dto.ApiKeyCreatedView;
import com.riskscoring.gateway.dto.CreateApiKeyRequest;
import com.riskscoring.gateway.entity.ApiKey;
import com.riskscoring.gateway.exception.ApiKeyLimitExceededException;
import com.riskscoring.gateway.exception.ApiKeyNotFoundException;
import com.riskscoring.gateway.exception.NoActiveSubscriptionException;
import com.riskscoring.gateway.mapper.ApiKeyMapper;
import com.riskscoring.gateway.model.ApiKeyStatus;
import com.riskscoring.gateway.model.PlanCode;
import com.riskscoring.gateway.repository.ApiKeyRepository;
import com.riskscoring.gateway.security.ApiKeyPrincipal;
import com.riskscoring.gateway.security.SecretGenerator;
import com.riskscoring.gateway.security.SecretHasher;
import com.riskscoring.gateway.service.BillingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ApiKeyRepository apiKeyRepository;
    @Mock
    private BillingService billingService;
    @Mock
    private SecretGenerator secretGenerator;

    @Captor
    private ArgumentCaptor<ApiKey> apiKeyCaptor;

    private ApiKeyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApiKeyServiceImpl(apiKeyRepository, new ApiKeyMapper(), new SecretHasher("pepper-1234567890123456"),
                secretGenerator, billingService, gatewayProperties());
    }

    @Test
    void createChecksActiveSubscriptionBeforeCreating() {
        doThrow(new NoActiveSubscriptionException()).when(billingService).requireActiveSubscription(USER_ID);

        assertThatThrownBy(() -> service.create(USER_ID, new CreateApiKeyRequest("CI key")))
                .isInstanceOf(NoActiveSubscriptionException.class);

        verifyNoInteractions(apiKeyRepository);
    }

    @Test
    void createThrowsApiKeyLimitExceededExceptionWhenAtLimit() {
        when(apiKeyRepository.countByUserIdAndStatus(USER_ID, ApiKeyStatus.ACTIVE)).thenReturn(5L);

        assertThatThrownBy(() -> service.create(USER_ID, new CreateApiKeyRequest("CI key")))
                .isInstanceOf(ApiKeyLimitExceededException.class);

        verify(apiKeyRepository, never()).save(any());
    }

    @Test
    void createReturnsPlaintextInViewButPersistsOnlyItsHash() {
        when(apiKeyRepository.countByUserIdAndStatus(USER_ID, ApiKeyStatus.ACTIVE)).thenReturn(0L);
        when(secretGenerator.generate(6)).thenReturn("public");
        when(secretGenerator.generate()).thenReturn("secretpart");
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeyCreatedView view = service.create(USER_ID, new CreateApiKeyRequest("CI key"));

        verify(apiKeyRepository).save(apiKeyCaptor.capture());
        ApiKey saved = apiKeyCaptor.getValue();
        String expectedPlaintext = "rsk_public_secretpart";
        assertThat(view.apiKey()).isEqualTo(expectedPlaintext);
        assertThat(saved.getKeyHash()).isNotEqualTo(expectedPlaintext);
        assertThat(saved.getKeyPrefix()).isEqualTo("rsk_public");
        assertThat(saved.getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void listMapsRepositoryKeysToViews() {
        ApiKey apiKey = apiKey(ApiKeyStatus.ACTIVE, null);
        when(apiKeyRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(apiKey));

        var views = service.list(USER_ID);

        assertThat(views).hasSize(1);
        assertThat(views.getFirst().id()).isEqualTo(apiKey.getId());
    }

    @Test
    void revokeThrowsApiKeyNotFoundExceptionWhenNotFoundOrNotOwned() {
        UUID keyId = UUID.randomUUID();
        when(apiKeyRepository.findByIdAndUserId(keyId, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revoke(USER_ID, keyId))
                .isInstanceOf(ApiKeyNotFoundException.class);
    }

    @Test
    void revokeIsNoOpWhenKeyIsAlreadyRevoked() {
        Instant revokedAt = Instant.now().minusSeconds(3600);
        ApiKey apiKey = apiKey(ApiKeyStatus.REVOKED, revokedAt);
        when(apiKeyRepository.findByIdAndUserId(apiKey.getId(), USER_ID)).thenReturn(Optional.of(apiKey));

        service.revoke(USER_ID, apiKey.getId());

        assertThat(apiKey.getRevokedAt()).isEqualTo(revokedAt);
    }

    @Test
    void revokeSetsRevokedStatusAndTimestampForActiveKey() {
        ApiKey apiKey = apiKey(ApiKeyStatus.ACTIVE, null);
        when(apiKeyRepository.findByIdAndUserId(apiKey.getId(), USER_ID)).thenReturn(Optional.of(apiKey));

        service.revoke(USER_ID, apiKey.getId());

        assertThat(apiKey.getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
        assertThat(apiKey.getRevokedAt()).isNotNull();
    }

    @Test
    void revokeAllActiveDelegatesToRepositoryWithActiveAndRevokedStatuses() {
        service.revokeAllActive(USER_ID);

        verify(apiKeyRepository).revokeAllActive(
                eq(USER_ID), any(Instant.class), eq(ApiKeyStatus.ACTIVE), eq(ApiKeyStatus.REVOKED));
    }

    @Test
    void resolveActiveKeyReturnsEmptyWhenHashNotFound() {
        when(apiKeyRepository.findByKeyHashAndStatus(any(), eq(ApiKeyStatus.ACTIVE))).thenReturn(Optional.empty());

        assertThat(service.resolveActiveKey("rsk_public_secretpart")).isEmpty();
    }

    @Test
    void resolveActiveKeyReturnsPrincipalAndTouchesLastUsedAtWhenFound() {
        ApiKey apiKey = apiKey(ApiKeyStatus.ACTIVE, null);
        when(apiKeyRepository.findByKeyHashAndStatus(any(), eq(ApiKeyStatus.ACTIVE))).thenReturn(Optional.of(apiKey));

        Optional<ApiKeyPrincipal> principal = service.resolveActiveKey("rsk_public_secretpart");

        assertThat(principal).contains(new ApiKeyPrincipal(apiKey.getUserId(), apiKey.getId()));
        verify(apiKeyRepository).touchLastUsedAt(eq(apiKey.getId()), any(Instant.class), any(Instant.class));
    }

    private static ApiKey apiKey(ApiKeyStatus status, Instant revokedAt) {
        return ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .name("CI key")
                .keyPrefix("rsk_public")
                .keyHash("hash")
                .status(status)
                .createdAt(Instant.now())
                .revokedAt(revokedAt)
                .build();
    }

    private static GatewayProperties gatewayProperties() {
        return new GatewayProperties(
                new GatewayProperties.Cors(List.of("http://localhost:5173")),
                new GatewayProperties.Auth("12345678901234567890123456789012", Duration.ofMinutes(15),
                        Duration.ofDays(30), 5, Duration.ofMinutes(15), false),
                new GatewayProperties.Mail("test@example.com", "contact@example.com"),
                new GatewayProperties.Verification("1234567890123456", Duration.ofMinutes(10),
                        Duration.ofSeconds(60), 5),
                new GatewayProperties.Billing(Duration.ofDays(30), List.of(
                        new GatewayProperties.Plan(PlanCode.FREE, 0, "USD", 10))),
                new GatewayProperties.ApiKeys("1234567890123456", "rsk_", 5, Duration.ofMinutes(5)),
                new GatewayProperties.PublicScan(new GatewayProperties.RateLimit(10, Duration.ofHours(1))),
                new GatewayProperties.Contact(new GatewayProperties.RateLimit(5, Duration.ofHours(1))),
                new GatewayProperties.PasswordReset(new GatewayProperties.RateLimit(5, Duration.ofHours(1)))
        );
    }
}
