package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.model.Language;
import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.entity.AppUser;
import com.riskscoring.gateway.entity.EmailVerificationCode;
import com.riskscoring.gateway.exception.InvalidVerificationCodeException;
import com.riskscoring.gateway.exception.ResendCooldownException;
import com.riskscoring.gateway.exception.TooManyVerificationAttemptsException;
import com.riskscoring.gateway.exception.VerificationCodeExpiredException;
import com.riskscoring.gateway.model.EmailCodePurpose;
import com.riskscoring.gateway.model.PlanCode;
import com.riskscoring.gateway.model.UserRole;
import com.riskscoring.gateway.model.UserStatus;
import com.riskscoring.gateway.repository.EmailVerificationCodeRepository;
import com.riskscoring.gateway.security.SecretHasher;
import com.riskscoring.gateway.service.EmailService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    private static final SecretHasher HASHER = new SecretHasher("pepper-1234567890123456");

    @Mock
    private EmailVerificationCodeRepository codeRepository;
    @Mock
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<EmailVerificationCode> codeCaptor;

    private EmailVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationServiceImpl(codeRepository, emailService, HASHER, gatewayProperties());
    }

    @Test
    void issueAndSendSendsVerificationEmailForRegistrationPurpose() {
        AppUser user = user();
        when(codeRepository.findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), EmailCodePurpose.REGISTRATION))
                .thenReturn(Optional.empty());

        service.issueAndSend(user, EmailCodePurpose.REGISTRATION);

        verify(emailService).sendVerificationCode(eq(user), any());
        verify(emailService, never()).sendPasswordResetCode(any(), any());
    }

    @Test
    void issueAndSendSendsPasswordResetEmailForPasswordResetPurpose() {
        AppUser user = user();
        when(codeRepository.findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), EmailCodePurpose.PASSWORD_RESET))
                .thenReturn(Optional.empty());

        service.issueAndSend(user, EmailCodePurpose.PASSWORD_RESET);

        verify(emailService).sendPasswordResetCode(eq(user), any());
        verify(emailService, never()).sendVerificationCode(any(), any());
    }

    @Test
    void issueAndSendMarksPreviousActiveCodeAsUsedBeforeSavingNewOne() {
        AppUser user = user();
        EmailVerificationCode previous = storedCode(user.getId(), "999999", Instant.now().plusSeconds(600), 0);
        when(codeRepository.findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), EmailCodePurpose.REGISTRATION))
                .thenReturn(Optional.of(previous));

        service.issueAndSend(user, EmailCodePurpose.REGISTRATION);

        verify(codeRepository).markUsed(eq(previous.getId()), any(Instant.class));
        verify(codeRepository).save(codeCaptor.capture());
        assertThat(codeCaptor.getValue().getUserId()).isEqualTo(user.getId());
    }

    @Test
    void issueAndSendDoesNotMarkAnythingUsedWhenNoPreviousCodeExists() {
        AppUser user = user();
        when(codeRepository.findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), EmailCodePurpose.REGISTRATION))
                .thenReturn(Optional.empty());

        service.issueAndSend(user, EmailCodePurpose.REGISTRATION);

        verify(codeRepository, never()).markUsed(any(), any());
    }

    @Test
    void resendThrowsResendCooldownExceptionWhenActiveCodeIsWithinCooldown() {
        AppUser user = user();
        EmailVerificationCode recent = storedCode(user.getId(), "999999", Instant.now().plusSeconds(600), 0);
        recent.setCreatedAt(Instant.now());
        when(codeRepository.findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), EmailCodePurpose.REGISTRATION))
                .thenReturn(Optional.of(recent));

        assertThatThrownBy(() -> service.resend(user, EmailCodePurpose.REGISTRATION))
                .isInstanceOf(ResendCooldownException.class);

        verifyNoInteractions(emailService);
    }

    @Test
    void resendIssuesNewCodeWhenNoActiveCodeExists() {
        AppUser user = user();
        when(codeRepository.findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), EmailCodePurpose.REGISTRATION))
                .thenReturn(Optional.empty());

        service.resend(user, EmailCodePurpose.REGISTRATION);

        verify(emailService).sendVerificationCode(eq(user), any());
    }

    @Test
    void resendIssuesNewCodeWhenCooldownHasElapsed() {
        AppUser user = user();
        EmailVerificationCode old = storedCode(user.getId(), "999999", Instant.now().plusSeconds(600), 0);
        old.setCreatedAt(Instant.now().minusSeconds(120));
        when(codeRepository.findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), EmailCodePurpose.REGISTRATION))
                .thenReturn(Optional.of(old));

        service.resend(user, EmailCodePurpose.REGISTRATION);

        verify(emailService).sendVerificationCode(eq(user), any());
    }

    @Test
    void verifyThrowsInvalidVerificationCodeExceptionWhenNoActiveCodeExists() {
        AppUser user = user();
        when(codeRepository.findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), EmailCodePurpose.REGISTRATION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(user, "123456", EmailCodePurpose.REGISTRATION))
                .isInstanceOf(InvalidVerificationCodeException.class);
    }

    @Test
    void verifyThrowsVerificationCodeExpiredExceptionWhenCodeIsExpired() {
        AppUser user = user();
        EmailVerificationCode stored = storedCode(user.getId(), "123456", Instant.now().minusSeconds(1), 0);
        when(codeRepository.findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), EmailCodePurpose.REGISTRATION))
                .thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.verify(user, "123456", EmailCodePurpose.REGISTRATION))
                .isInstanceOf(VerificationCodeExpiredException.class);
    }

    @Test
    void verifyThrowsTooManyVerificationAttemptsExceptionWhenAttemptsAlreadyAtMax() {
        AppUser user = user();
        EmailVerificationCode stored = storedCode(user.getId(), "123456", Instant.now().plusSeconds(600), 5);
        when(codeRepository.findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), EmailCodePurpose.REGISTRATION))
                .thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.verify(user, "123456", EmailCodePurpose.REGISTRATION))
                .isInstanceOf(TooManyVerificationAttemptsException.class);

        verify(codeRepository, never()).incrementAttempts(any(), anyInt());
    }

    @Test
    void verifyIncrementsAttemptsAndThrowsInvalidVerificationCodeExceptionWhenCodeIsWrong() {
        AppUser user = user();
        EmailVerificationCode stored = storedCode(user.getId(), "123456", Instant.now().plusSeconds(600), 1);
        when(codeRepository.findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), EmailCodePurpose.REGISTRATION))
                .thenReturn(Optional.of(stored));
        when(codeRepository.incrementAttempts(stored.getId(), 5)).thenReturn(1);

        assertThatThrownBy(() -> service.verify(user, "000000", EmailCodePurpose.REGISTRATION))
                .isInstanceOf(InvalidVerificationCodeException.class);
    }

    @Test
    void verifyThrowsTooManyVerificationAttemptsExceptionWhenIncrementRacesToZero() {
        AppUser user = user();
        EmailVerificationCode stored = storedCode(user.getId(), "123456", Instant.now().plusSeconds(600), 4);
        when(codeRepository.findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), EmailCodePurpose.REGISTRATION))
                .thenReturn(Optional.of(stored));
        when(codeRepository.incrementAttempts(stored.getId(), 5)).thenReturn(0);

        assertThatThrownBy(() -> service.verify(user, "000000", EmailCodePurpose.REGISTRATION))
                .isInstanceOf(TooManyVerificationAttemptsException.class);
    }

    @Test
    void verifyMarksCodeUsedWhenCodeIsCorrect() {
        AppUser user = user();
        EmailVerificationCode stored = storedCode(user.getId(), "123456", Instant.now().plusSeconds(600), 0);
        when(codeRepository.findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), EmailCodePurpose.REGISTRATION))
                .thenReturn(Optional.of(stored));
        when(codeRepository.markUsed(eq(stored.getId()), any(Instant.class))).thenReturn(1);

        service.verify(user, "123456", EmailCodePurpose.REGISTRATION);

        verify(codeRepository).markUsed(eq(stored.getId()), any(Instant.class));
    }

    @Test
    void verifyThrowsInvalidVerificationCodeExceptionWhenMarkUsedRacesToZero() {
        AppUser user = user();
        EmailVerificationCode stored = storedCode(user.getId(), "123456", Instant.now().plusSeconds(600), 0);
        when(codeRepository.findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), EmailCodePurpose.REGISTRATION))
                .thenReturn(Optional.of(stored));
        when(codeRepository.markUsed(eq(stored.getId()), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> service.verify(user, "123456", EmailCodePurpose.REGISTRATION))
                .isInstanceOf(InvalidVerificationCodeException.class);
    }

    private static AppUser user() {
        Instant now = Instant.now();
        return AppUser.builder()
                .id(UUID.randomUUID())
                .username("jane")
                .email("jane@example.com")
                .passwordHash("hash")
                .firstName("Jane")
                .lastName("Doe")
                .role(UserRole.USER)
                .status(UserStatus.PENDING_VERIFICATION)
                .language(Language.EN)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static EmailVerificationCode storedCode(UUID userId, String code, Instant expiresAt, int attempts) {
        return EmailVerificationCode.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .codeHash(HASHER.hash(code))
                .purpose(EmailCodePurpose.REGISTRATION)
                .expiresAt(expiresAt)
                .attempts(attempts)
                .createdAt(Instant.now().minusSeconds(120))
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
