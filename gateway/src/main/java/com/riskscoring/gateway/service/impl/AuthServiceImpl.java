package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.model.Language;
import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.dto.IssuedSession;
import com.riskscoring.gateway.dto.LoginRequest;
import com.riskscoring.gateway.dto.RegisterRequest;
import com.riskscoring.gateway.dto.RegistrationResponse;
import com.riskscoring.gateway.dto.ResetPasswordRequest;
import com.riskscoring.gateway.dto.UserView;
import com.riskscoring.gateway.dto.VerifyEmailRequest;
import com.riskscoring.gateway.entity.AppUser;
import com.riskscoring.gateway.exception.AccountLockedException;
import com.riskscoring.gateway.exception.AccountNotActiveException;
import com.riskscoring.gateway.exception.ApiException;
import com.riskscoring.gateway.exception.EmailAlreadyRegisteredException;
import com.riskscoring.gateway.exception.InvalidCredentialsException;
import com.riskscoring.gateway.exception.InvalidVerificationCodeException;
import com.riskscoring.gateway.exception.ResendCooldownException;
import com.riskscoring.gateway.exception.TooManyVerificationAttemptsException;
import com.riskscoring.gateway.exception.UnauthorizedException;
import com.riskscoring.gateway.exception.UsernameAlreadyTakenException;
import com.riskscoring.gateway.exception.VerificationCodeExpiredException;
import com.riskscoring.gateway.mapper.UserMapper;
import com.riskscoring.gateway.model.EmailCodePurpose;
import com.riskscoring.gateway.model.UserRole;
import com.riskscoring.gateway.model.UserStatus;
import com.riskscoring.gateway.repository.AppUserRepository;
import com.riskscoring.gateway.repository.RefreshTokenRepository;
import com.riskscoring.gateway.service.AuthService;
import com.riskscoring.gateway.service.EmailVerificationService;
import com.riskscoring.gateway.service.RateLimitService;
import com.riskscoring.gateway.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String ABSENT_USER_HASH = "$2y$12$LBfmIlKspHraK85UGm5Mgur8Tw3Ean5BHAuPpC02TvKIDOf166S5y";
    private static final String UNIQUE_EMAIL_CONSTRAINT = "uq_app_user_email";
    private static final String UNIQUE_USERNAME_CONSTRAINT = "uq_app_user_username";

    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final GatewayProperties gatewayProperties;
    private final RateLimitService rateLimitService;

    @Override
    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String username = request.username().trim();

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameAlreadyTakenException();
        }

        Instant now = Instant.now();
        AppUser user = AppUser.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .role(UserRole.USER)
                .status(UserStatus.PENDING_VERIFICATION)
                .tokenVersion(0)
                .failedLoginAttempts(0)
                .language(Language.fromLocale(LocaleContextHolder.getLocale()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            appUserRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw translateDuplicateUser(exception);
        }

        emailVerificationService.issueAndSend(user, EmailCodePurpose.REGISTRATION);
        log.info("Registered user {} pending email verification", user.getId());

        return new RegistrationResponse(user.getEmail(), user.getStatus());
    }

    @Override
    @Transactional(noRollbackFor = InvalidVerificationCodeException.class)
    public IssuedSession verifyEmail(VerifyEmailRequest request, String userAgent, String ipAddress) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(InvalidVerificationCodeException::new);

        emailVerificationService.verify(user, request.code(), EmailCodePurpose.REGISTRATION);

        Instant now = Instant.now();
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(now);
        user.setUpdatedAt(now);

        return issueSession(user, userAgent, ipAddress);
    }

    @Override
    @Transactional
    public void resendVerificationCode(String email) {
        appUserRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(user -> user.getStatus() == UserStatus.PENDING_VERIFICATION)
                .ifPresent(user -> emailVerificationService.resend(user, EmailCodePurpose.REGISTRATION));
    }

    @Override
    @Transactional
    public void forgotPassword(String email, String clientIp) {
        rateLimitService.checkPasswordReset(clientIp);

        appUserRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(user -> user.getStatus() != UserStatus.BLOCKED)
                .ifPresent(user -> {
                    try {
                        emailVerificationService.resend(user, EmailCodePurpose.PASSWORD_RESET);
                    } catch (ResendCooldownException ignored) {
                        // response must look identical regardless of cooldown state
                    }
                });
    }

    @Override
    @Transactional(noRollbackFor = {
            InvalidVerificationCodeException.class,
            VerificationCodeExpiredException.class,
            TooManyVerificationAttemptsException.class
    })
    public IssuedSession resetPassword(ResetPasswordRequest request, String userAgent, String ipAddress) {
        rateLimitService.checkPasswordReset(ipAddress);

        AppUser user = appUserRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(InvalidVerificationCodeException::new);

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountNotActiveException(user.getStatus());
        }

        emailVerificationService.verify(user, request.code(), EmailCodePurpose.PASSWORD_RESET);

        Instant now = Instant.now();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setUpdatedAt(now);
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
            user.setEmailVerifiedAt(now);
        }

        refreshTokenRepository.revokeAllForUser(user.getId(), now);
        log.info("Password reset for user {}", user.getId());

        return issueSession(user, userAgent, ipAddress);
    }

    @Override
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public IssuedSession login(LoginRequest request, String userAgent, String ipAddress) {
        Optional<AppUser> found = findByLogin(request.login());

        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), ABSENT_USER_HASH);
            throw new InvalidCredentialsException();
        }

        AppUser user = found.get();
        ensureNotLocked(user);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            throw new InvalidCredentialsException();
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountNotActiveException(user.getStatus());
        }

        resetFailedAttempts(user);

        return issueSession(user, userAgent, ipAddress);
    }

    @Override
    @Transactional
    public IssuedSession refresh(String rawRefreshToken, String userAgent, String ipAddress) {
        AppUser user = tokenService.consumeRefreshToken(rawRefreshToken);

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountNotActiveException(user.getStatus());
        }

        return issueSession(user, userAgent, ipAddress);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        tokenService.revokeRefreshToken(rawRefreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public UserView currentUser(UUID userId) {
        return appUserRepository.findById(userId)
                .map(userMapper::toView)
                .orElseThrow(UnauthorizedException::new);
    }

    private Optional<AppUser> findByLogin(String login) {
        String normalized = login.trim();
        return appUserRepository.findByEmailIgnoreCase(normalized)
                .or(() -> appUserRepository.findByUsernameIgnoreCase(normalized));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ApiException translateDuplicateUser(DataIntegrityViolationException exception) {
        String constraintName = exception.getCause() instanceof ConstraintViolationException cve
                ? cve.getConstraintName()
                : null;

        if (UNIQUE_EMAIL_CONSTRAINT.equals(constraintName)) {
            return new EmailAlreadyRegisteredException();
        }
        if (UNIQUE_USERNAME_CONSTRAINT.equals(constraintName)) {
            return new UsernameAlreadyTakenException();
        }
        throw exception;
    }

    private void ensureNotLocked(AppUser user) {
        Instant lockedUntil = user.getLockedUntil();
        if (lockedUntil != null && lockedUntil.isAfter(Instant.now())) {
            throw new AccountLockedException(lockedUntil);
        }
    }

    private void registerFailedAttempt(AppUser user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setUpdatedAt(Instant.now());

        if (attempts >= gatewayProperties.auth().maxFailedLoginAttempts()) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(Instant.now().plus(gatewayProperties.auth().lockoutDuration()));
            log.warn("Account {} locked after {} failed login attempts", user.getId(), attempts);
        } else {
            user.setFailedLoginAttempts(attempts);
        }
    }

    private void resetFailedAttempts(AppUser user) {
        if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            user.setUpdatedAt(Instant.now());
        }
    }

    private IssuedSession issueSession(AppUser user, String userAgent, String ipAddress) {
        return new IssuedSession(
                tokenService.issueAccessToken(user),
                tokenService.issueRefreshToken(user, userAgent, ipAddress),
                userMapper.toView(user)
        );
    }
}
