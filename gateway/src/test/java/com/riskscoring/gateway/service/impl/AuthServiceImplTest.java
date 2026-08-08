package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.model.Language;
import com.riskscoring.gateway.dto.IssuedSession;
import com.riskscoring.gateway.dto.LoginRequest;
import com.riskscoring.gateway.dto.RegisterRequest;
import com.riskscoring.gateway.dto.RegistrationResponse;
import com.riskscoring.gateway.dto.ResetPasswordRequest;
import com.riskscoring.gateway.dto.VerifyEmailRequest;
import com.riskscoring.gateway.entity.AppUser;
import com.riskscoring.gateway.exception.AccountLockedException;
import com.riskscoring.gateway.exception.AccountNotActiveException;
import com.riskscoring.gateway.exception.EmailAlreadyRegisteredException;
import com.riskscoring.gateway.exception.InvalidCredentialsException;
import com.riskscoring.gateway.exception.InvalidVerificationCodeException;
import com.riskscoring.gateway.exception.RateLimitExceededException;
import com.riskscoring.gateway.exception.UnauthorizedException;
import com.riskscoring.gateway.exception.UsernameAlreadyTakenException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.riskscoring.gateway.support.GatewayPropertiesFixture.gatewayProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Test
    void registerNormalizesEmailAndUsernameAndCreatesPendingVerificationUser() {
        when(appUserRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(appUserRepository.existsByUsernameIgnoreCase("JaneDoe")).thenReturn(false);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        RegistrationResponse response = authService.register(
                new RegisterRequest(" JaneDoe ", "Jane", "Doe", "  USER@EXAMPLE.COM  ", NEW_PASSWORD));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).saveAndFlush(captor.capture());
        AppUser saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getUsername()).isEqualTo("JaneDoe");
        assertThat(saved.getPasswordHash()).isEqualTo(ENCODED_PASSWORD);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        verify(emailVerificationService).issueAndSend(saved, EmailCodePurpose.REGISTRATION);
        assertThat(response).isEqualTo(new RegistrationResponse(EMAIL, UserStatus.PENDING_VERIFICATION));
    }

    @Test
    void registerThrowsEmailAlreadyRegisteredExceptionWhenEmailExists() {
        when(appUserRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(appUserRepository, never()).saveAndFlush(any());
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void registerThrowsUsernameAlreadyTakenExceptionWhenUsernameExists() {
        when(appUserRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(appUserRepository.existsByUsernameIgnoreCase("JaneDoe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(UsernameAlreadyTakenException.class);
    }

    @Test
    void registerTranslatesEmailUniqueConstraintViolationToEmailAlreadyRegisteredException() {
        when(appUserRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(appUserRepository.existsByUsernameIgnoreCase("JaneDoe")).thenReturn(false);
        when(appUserRepository.saveAndFlush(any())).thenThrow(constraintViolation("uq_app_user_email"));

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void registerTranslatesUsernameUniqueConstraintViolationToUsernameAlreadyTakenException() {
        when(appUserRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(appUserRepository.existsByUsernameIgnoreCase("JaneDoe")).thenReturn(false);
        when(appUserRepository.saveAndFlush(any())).thenThrow(constraintViolation("uq_app_user_username"));

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(UsernameAlreadyTakenException.class);
    }

    @Test
    void registerRethrowsDataIntegrityViolationExceptionForUnrelatedConstraint() {
        when(appUserRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(appUserRepository.existsByUsernameIgnoreCase("JaneDoe")).thenReturn(false);
        DataIntegrityViolationException unrelated = constraintViolation("some_other_constraint");
        when(appUserRepository.saveAndFlush(any())).thenThrow(unrelated);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isSameAs(unrelated);
    }

    @Test
    void verifyEmailThrowsInvalidVerificationCodeExceptionForUnknownEmail() {
        when(appUserRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail(verifyEmailRequest(), USER_AGENT, CLIENT_IP))
                .isInstanceOf(InvalidVerificationCodeException.class);

        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void verifyEmailActivatesUserAndIssuesSession() {
        AppUser pending = user(UserStatus.PENDING_VERIFICATION);
        when(appUserRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(pending));
        when(tokenService.issueAccessToken(pending)).thenReturn("access-token");
        when(tokenService.issueRefreshToken(eq(pending), any(), any())).thenReturn("refresh-token");

        IssuedSession session = authService.verifyEmail(verifyEmailRequest(), USER_AGENT, CLIENT_IP);

        verify(emailVerificationService).verify(pending, CODE, EmailCodePurpose.REGISTRATION);
        assertThat(pending.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(pending.getEmailVerifiedAt()).isNotNull();
        assertThat(session.accessToken()).isEqualTo("access-token");
    }

    @Test
    void resendVerificationCodeResendsWhenUserIsPendingVerification() {
        AppUser pending = user(UserStatus.PENDING_VERIFICATION);
        when(appUserRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(pending));

        authService.resendVerificationCode(EMAIL);

        verify(emailVerificationService).resend(pending, EmailCodePurpose.REGISTRATION);
    }

    @Test
    void resendVerificationCodeIsSilentForUnknownEmail() {
        when(appUserRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        authService.resendVerificationCode(EMAIL);

        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void resendVerificationCodeIsSilentWhenUserIsNotPendingVerification() {
        AppUser active = user(UserStatus.ACTIVE);
        when(appUserRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(active));

        authService.resendVerificationCode(EMAIL);

        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void loginThrowsInvalidCredentialsExceptionForUnknownLoginAfterTimingAttackMitigation() {
        when(appUserRepository.findByEmailIgnoreCase("unknown")).thenReturn(Optional.empty());
        when(appUserRepository.findByUsernameIgnoreCase("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown", "whatever"), USER_AGENT, CLIENT_IP))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder).matches(eq("whatever"), anyString());
    }

    @Test
    void loginThrowsAccountLockedExceptionWhenUserIsCurrentlyLocked() {
        AppUser locked = user(UserStatus.ACTIVE);
        locked.setLockedUntil(Instant.now().plusSeconds(300));
        when(appUserRepository.findByEmailIgnoreCase("user")).thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> authService.login(new LoginRequest("user", "whatever"), USER_AGENT, CLIENT_IP))
                .isInstanceOf(AccountLockedException.class);

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void loginRegistersFailedAttemptAndThrowsInvalidCredentialsExceptionOnWrongPassword() {
        AppUser active = user(UserStatus.ACTIVE);
        when(appUserRepository.findByEmailIgnoreCase("user")).thenReturn(Optional.of(active));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user", "wrong"), USER_AGENT, CLIENT_IP))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(active.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(active.getLockedUntil()).isNull();
    }

    @Test
    void loginLocksAccountWhenFailedAttemptsReachConfiguredThreshold() {
        AppUser active = user(UserStatus.ACTIVE);
        active.setFailedLoginAttempts(4);
        when(appUserRepository.findByEmailIgnoreCase("user")).thenReturn(Optional.of(active));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user", "wrong"), USER_AGENT, CLIENT_IP))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(active.getFailedLoginAttempts()).isZero();
        assertThat(active.getLockedUntil()).isNotNull();
    }

    @Test
    void loginThrowsAccountNotActiveExceptionWhenPasswordCorrectButUserNotActive() {
        AppUser pending = user(UserStatus.PENDING_VERIFICATION);
        when(appUserRepository.findByEmailIgnoreCase("user")).thenReturn(Optional.of(pending));
        when(passwordEncoder.matches("correct", "old-hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user", "correct"), USER_AGENT, CLIENT_IP))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    void loginResetsFailedAttemptsAndIssuesSessionOnSuccess() {
        AppUser active = user(UserStatus.ACTIVE);
        active.setFailedLoginAttempts(3);
        when(appUserRepository.findByEmailIgnoreCase("user")).thenReturn(Optional.of(active));
        when(passwordEncoder.matches("correct", "old-hash")).thenReturn(true);
        when(tokenService.issueAccessToken(active)).thenReturn("access-token");
        when(tokenService.issueRefreshToken(eq(active), any(), any())).thenReturn("refresh-token");

        IssuedSession session = authService.login(new LoginRequest("user", "correct"), USER_AGENT, CLIENT_IP);

        assertThat(active.getFailedLoginAttempts()).isZero();
        assertThat(session.accessToken()).isEqualTo("access-token");
    }

    @Test
    void refreshThrowsAccountNotActiveExceptionWhenConsumedTokenBelongsToInactiveUser() {
        AppUser blocked = user(UserStatus.BLOCKED);
        when(tokenService.consumeRefreshToken("raw-token")).thenReturn(blocked);

        assertThatThrownBy(() -> authService.refresh("raw-token", USER_AGENT, CLIENT_IP))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    void refreshIssuesNewSessionForActiveUser() {
        AppUser active = user(UserStatus.ACTIVE);
        when(tokenService.consumeRefreshToken("raw-token")).thenReturn(active);
        when(tokenService.issueAccessToken(active)).thenReturn("access-token");
        when(tokenService.issueRefreshToken(eq(active), any(), any())).thenReturn("refresh-token");

        IssuedSession session = authService.refresh("raw-token", USER_AGENT, CLIENT_IP);

        assertThat(session.accessToken()).isEqualTo("access-token");
        assertThat(session.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void logoutDelegatesToTokenServiceRevokeRefreshToken() {
        authService.logout("raw-token");

        verify(tokenService).revokeRefreshToken("raw-token");
    }

    @Test
    void currentUserReturnsMappedViewWhenUserFound() {
        AppUser active = user(UserStatus.ACTIVE);
        when(appUserRepository.findById(active.getId())).thenReturn(Optional.of(active));

        var view = authService.currentUser(active.getId());

        assertThat(view.id()).isEqualTo(active.getId());
        assertThat(view.username()).isEqualTo(active.getUsername());
    }

    @Test
    void currentUserThrowsUnauthorizedExceptionWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(appUserRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.currentUser(userId))
                .isInstanceOf(UnauthorizedException.class);
    }

    private static DataIntegrityViolationException constraintViolation(String constraintName) {
        return new DataIntegrityViolationException("duplicate",
                new org.hibernate.exception.ConstraintViolationException(
                        "duplicate", new java.sql.SQLException("duplicate"), constraintName));
    }

    private static RegisterRequest registerRequest() {
        return new RegisterRequest("JaneDoe", "Jane", "Doe", EMAIL, NEW_PASSWORD);
    }

    private static VerifyEmailRequest verifyEmailRequest() {
        return new VerifyEmailRequest(EMAIL, CODE);
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

}
