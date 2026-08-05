package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.model.Language;
import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.dto.IssuedSession;
import com.riskscoring.gateway.dto.ResetPasswordRequest;
import com.riskscoring.gateway.entity.AppUser;
import com.riskscoring.gateway.exception.AccountNotActiveException;
import com.riskscoring.gateway.exception.InvalidVerificationCodeException;
import com.riskscoring.gateway.exception.RateLimitExceededException;
import com.riskscoring.gateway.mapper.UserMapper;
import com.riskscoring.gateway.model.EmailCodePurpose;
import com.riskscoring.gateway.model.UserRole;
import com.riskscoring.gateway.model.UserStatus;
import com.riskscoring.gateway.repository.AppUserRepository;
import com.riskscoring.gateway.repository.RefreshTokenRepository;
import com.riskscoring.gateway.service.EmailVerificationService;
import com.riskscoring.gateway.service.RateLimitService;
import com.riskscoring.gateway.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
class AuthServiceImplTest {

    private static final String CLIENT_IP = "203.0.113.10";
    private static final String USER_AGENT = "junit";
    private static final String EMAIL = "user@example.com";
    private static final String CODE = "123456";
    private static final String NEW_PASSWORD = "correct-horse-1";
    private static final String ENCODED_PASSWORD = "encoded";

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private TokenService tokenService;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RateLimitService rateLimitService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                appUserRepository,
                refreshTokenRepository,
                tokenService,
                emailVerificationService,
                passwordEncoder,
                new UserMapper(),
                gatewayProperties(),
                rateLimitService);
    }

    @Test
    void forgotPasswordChecksRateLimitBeforeLookingUpUser() {
        doThrow(new RateLimitExceededException(60)).when(rateLimitService).checkPasswordReset(CLIENT_IP);

        assertThatThrownBy(() -> authService.forgotPassword(EMAIL, CLIENT_IP))
                .isInstanceOf(RateLimitExceededException.class);

        verifyNoInteractions(appUserRepository);
    }

    @Test
    void forgotPasswordIsSilentForUnknownEmail() {
        when(appUserRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        authService.forgotPassword(EMAIL, CLIENT_IP);

        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void forgotPasswordIsSilentForBlockedUser() {
        AppUser blocked = user(UserStatus.BLOCKED);
        when(appUserRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(blocked));

        authService.forgotPassword(EMAIL, CLIENT_IP);

        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void forgotPasswordIssuesResetCodeForActiveUser() {
        AppUser active = user(UserStatus.ACTIVE);
        when(appUserRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(active));

        authService.forgotPassword(EMAIL, CLIENT_IP);

        verify(emailVerificationService).resend(active, EmailCodePurpose.PASSWORD_RESET);
    }

    @Test
    void resetPasswordRejectsBlockedUserWithoutBurningTheCode() {
        AppUser blocked = user(UserStatus.BLOCKED);
        when(appUserRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(blocked));

        assertThatThrownBy(() -> authService.resetPassword(resetRequest(), USER_AGENT, CLIENT_IP))
                .isInstanceOf(AccountNotActiveException.class);

        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void resetPasswordRejectsUnknownEmail() {
        when(appUserRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(resetRequest(), USER_AGENT, CLIENT_IP))
                .isInstanceOf(InvalidVerificationCodeException.class);
    }

    @Test
    void resetPasswordRotatesCredentialsAndRevokesExistingSessions() {
        AppUser active = user(UserStatus.ACTIVE);
        active.setTokenVersion(3);
        when(appUserRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(active));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(tokenService.issueAccessToken(active)).thenReturn("access-token");
        when(tokenService.issueRefreshToken(eq(active), any(), any())).thenReturn("refresh-token");

        IssuedSession session = authService.resetPassword(resetRequest(), USER_AGENT, CLIENT_IP);

        verify(emailVerificationService).verify(active, CODE, EmailCodePurpose.PASSWORD_RESET);
        verify(refreshTokenRepository).revokeAllForUser(eq(active.getId()), any(Instant.class));
        assertThat(active.getPasswordHash()).isEqualTo(ENCODED_PASSWORD);
        assertThat(active.getTokenVersion()).isEqualTo(4);
        assertThat(session.accessToken()).isEqualTo("access-token");
        assertThat(session.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void resetPasswordActivatesPendingVerificationUser() {
        AppUser pending = user(UserStatus.PENDING_VERIFICATION);
        when(appUserRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(pending));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(tokenService.issueAccessToken(pending)).thenReturn("access-token");
        when(tokenService.issueRefreshToken(eq(pending), any(), any())).thenReturn("refresh-token");

        authService.resetPassword(resetRequest(), USER_AGENT, CLIENT_IP);

        assertThat(pending.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(pending.getEmailVerifiedAt()).isNotNull();
    }

    private static ResetPasswordRequest resetRequest() {
        return new ResetPasswordRequest(EMAIL, CODE, NEW_PASSWORD);
    }

    private static AppUser user(UserStatus status) {
        Instant now = Instant.now();
        return AppUser.builder()
                .id(UUID.randomUUID())
                .username("user")
                .email(EMAIL)
                .passwordHash("old-hash")
                .firstName("Jane")
                .lastName("Doe")
                .role(UserRole.USER)
                .status(status)
                .tokenVersion(0)
                .failedLoginAttempts(0)
                .language(Language.EN)
                .createdAt(now)
                .updatedAt(now)
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
                        new GatewayProperties.Plan(com.riskscoring.gateway.model.PlanCode.FREE, 0, "USD", 10)
                )),
                new GatewayProperties.ApiKeys("1234567890123456", "rsk_", 5, Duration.ofMinutes(5)),
                new GatewayProperties.PublicScan(new GatewayProperties.RateLimit(10, Duration.ofHours(1))),
                new GatewayProperties.Contact(new GatewayProperties.RateLimit(5, Duration.ofHours(1))),
                new GatewayProperties.PasswordReset(new GatewayProperties.RateLimit(5, Duration.ofHours(1)))
        );
    }
}
