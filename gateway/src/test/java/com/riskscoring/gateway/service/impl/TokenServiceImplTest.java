package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.model.Language;
import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.entity.AppUser;
import com.riskscoring.gateway.entity.RefreshToken;
import com.riskscoring.gateway.exception.InvalidRefreshTokenException;
import com.riskscoring.gateway.model.PlanCode;
import com.riskscoring.gateway.model.UserRole;
import com.riskscoring.gateway.model.UserStatus;
import com.riskscoring.gateway.repository.AppUserRepository;
import com.riskscoring.gateway.repository.RefreshTokenRepository;
import com.riskscoring.gateway.security.AuthenticatedUser;
import com.riskscoring.gateway.security.SecretGenerator;
import com.riskscoring.gateway.security.SecretHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private JwtEncoder jwtEncoder;
    @Mock
    private JwtDecoder jwtDecoder;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private SecretGenerator secretGenerator;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    private TokenServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TokenServiceImpl(jwtEncoder, jwtDecoder, appUserRepository, refreshTokenRepository,
                new SecretHasher("pepper-1234567890123456"), secretGenerator, gatewayProperties());
    }

    @Test
    void issueAccessTokenEncodesClaimsWithSubjectVersionAndRole() {
        AppUser user = user(UserRole.ADMIN, 3, UserStatus.ACTIVE);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("access-token-value");
        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        when(jwtEncoder.encode(captor.capture())).thenReturn(jwt);

        String token = service.issueAccessToken(user);

        assertThat(token).isEqualTo("access-token-value");
        JwtClaimsSet claims = captor.getValue().getClaims();
        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.getClaimAsString("typ")).isEqualTo("access");
        assertThat(claims.<Integer>getClaim("ver")).isEqualTo(3);
        assertThat(claims.getClaimAsString("role")).isEqualTo("ADMIN");
    }

    @Test
    void resolveAccessTokenReturnsEmptyWhenDecodeThrows() {
        when(jwtDecoder.decode("bad")).thenThrow(new JwtException("bad"));

        assertThat(service.resolveAccessToken("bad")).isEmpty();
    }

    @Test
    void resolveAccessTokenReturnsEmptyWhenTypeClaimIsNotAccess() {
        when(jwtDecoder.decode("t")).thenReturn(jwt(UUID.randomUUID().toString(), "refresh", 0));

        assertThat(service.resolveAccessToken("t")).isEmpty();
    }

    @Test
    void resolveAccessTokenReturnsEmptyWhenVersionClaimMissing() {
        when(jwtDecoder.decode("t")).thenReturn(jwt(UUID.randomUUID().toString(), "access", null));

        assertThat(service.resolveAccessToken("t")).isEmpty();
    }

    @Test
    void resolveAccessTokenReturnsEmptyWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(jwtDecoder.decode("t")).thenReturn(jwt(userId.toString(), "access", 0));
        when(appUserRepository.findById(userId)).thenReturn(Optional.empty());

        assertThat(service.resolveAccessToken("t")).isEmpty();
    }

    @Test
    void resolveAccessTokenReturnsEmptyWhenUserIsNotActive() {
        AppUser user = user(UserRole.USER, 0, UserStatus.BLOCKED);
        when(jwtDecoder.decode("t")).thenReturn(jwt(user.getId().toString(), "access", 0));
        when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThat(service.resolveAccessToken("t")).isEmpty();
    }

    @Test
    void resolveAccessTokenReturnsEmptyWhenTokenVersionMismatches() {
        AppUser user = user(UserRole.USER, 5, UserStatus.ACTIVE);
        when(jwtDecoder.decode("t")).thenReturn(jwt(user.getId().toString(), "access", 3));
        when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThat(service.resolveAccessToken("t")).isEmpty();
    }

    @Test
    void resolveAccessTokenReturnsAuthenticatedUserWhenAllChecksPass() {
        AppUser user = user(UserRole.ADMIN, 5, UserStatus.ACTIVE);
        when(jwtDecoder.decode("t")).thenReturn(jwt(user.getId().toString(), "access", 5));
        when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));

        Optional<AuthenticatedUser> result = service.resolveAccessToken("t");

        assertThat(result).contains(new AuthenticatedUser(user.getId(), user.getUsername(), UserRole.ADMIN));
    }

    @Test
    void issueRefreshTokenPersistsHashedTokenAndReturnsRawToken() {
        AppUser user = user(UserRole.USER, 0, UserStatus.ACTIVE);
        when(secretGenerator.generate()).thenReturn("raw-refresh-token");

        String token = service.issueRefreshToken(user, "short-agent", "203.0.113.10");

        assertThat(token).isEqualTo("raw-refresh-token");
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        RefreshToken saved = refreshTokenCaptor.getValue();
        assertThat(saved.getTokenHash()).isNotEqualTo("raw-refresh-token");
        assertThat(saved.getUserId()).isEqualTo(user.getId());
        assertThat(saved.getUserAgent()).isEqualTo("short-agent");
        assertThat(saved.getIpAddress()).isEqualTo("203.0.113.10");
    }

    @Test
    void issueRefreshTokenTruncatesUserAgentLongerThan255Characters() {
        AppUser user = user(UserRole.USER, 0, UserStatus.ACTIVE);
        when(secretGenerator.generate()).thenReturn("raw-refresh-token");

        service.issueRefreshToken(user, "a".repeat(300), "203.0.113.10");

        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        assertThat(refreshTokenCaptor.getValue().getUserAgent()).hasSize(255);
    }

    @Test
    void issueRefreshTokenKeepsNullUserAgentAsNull() {
        AppUser user = user(UserRole.USER, 0, UserStatus.ACTIVE);
        when(secretGenerator.generate()).thenReturn("raw-refresh-token");

        service.issueRefreshToken(user, null, "203.0.113.10");

        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        assertThat(refreshTokenCaptor.getValue().getUserAgent()).isNull();
    }

    @Test
    void consumeRefreshTokenThrowsWhenRawTokenIsNullOrBlank() {
        assertThatThrownBy(() -> service.consumeRefreshToken(null)).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.consumeRefreshToken("   ")).isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void consumeRefreshTokenThrowsWhenHashIsNotFound() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consumeRefreshToken("raw")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void consumeRefreshTokenThrowsWhenTokenIsAlreadyRevoked() {
        RefreshToken token = refreshToken(UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consumeRefreshToken("raw")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void consumeRefreshTokenThrowsWhenTokenIsExpired() {
        RefreshToken token = refreshToken(UUID.randomUUID(), null, Instant.now().minusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consumeRefreshToken("raw")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void consumeRefreshTokenThrowsWhenOwningUserNoLongerExists() {
        RefreshToken token = refreshToken(UUID.randomUUID(), null, Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(appUserRepository.findById(token.getUserId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consumeRefreshToken("raw")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void consumeRefreshTokenMarksTokenRevokedAndReturnsOwningUser() {
        AppUser user = user(UserRole.USER, 0, UserStatus.ACTIVE);
        RefreshToken token = refreshToken(user.getId(), null, Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));

        AppUser result = service.consumeRefreshToken("raw");

        assertThat(result).isEqualTo(user);
        assertThat(token.getRevokedAt()).isNotNull();
    }

    @Test
    void revokeRefreshTokenMarksActiveTokenRevoked() {
        RefreshToken token = refreshToken(UUID.randomUUID(), null, Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        service.revokeRefreshToken("raw");

        assertThat(token.getRevokedAt()).isNotNull();
    }

    @Test
    void revokeRefreshTokenDoesNothingWhenTokenIsNotFound() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatCode(() -> service.revokeRefreshToken("raw")).doesNotThrowAnyException();
    }

    private static Jwt jwt(String subject, String type, Integer version) {
        Jwt.Builder builder = Jwt.withTokenValue("raw")
                .header("alg", "HS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
        if (subject != null) {
            builder.claim("sub", subject);
        }
        if (type != null) {
            builder.claim("typ", type);
        }
        if (version != null) {
            builder.claim("ver", version);
        }
        return builder.build();
    }

    private static AppUser user(UserRole role, int tokenVersion, UserStatus status) {
        Instant now = Instant.now();
        return AppUser.builder()
                .id(UUID.randomUUID())
                .username("jane")
                .email("jane@example.com")
                .passwordHash("hash")
                .firstName("Jane")
                .lastName("Doe")
                .role(role)
                .status(status)
                .tokenVersion(tokenVersion)
                .failedLoginAttempts(0)
                .language(Language.EN)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static RefreshToken refreshToken(UUID userId, Instant revokedAt, Instant expiresAt) {
        return RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("hash")
                .issuedAt(Instant.now())
                .expiresAt(expiresAt)
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
