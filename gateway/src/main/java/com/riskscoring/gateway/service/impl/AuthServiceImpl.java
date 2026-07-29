package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.model.Language;
import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.dto.IssuedSession;
import com.riskscoring.gateway.dto.LoginRequest;
import com.riskscoring.gateway.dto.RegisterRequest;
import com.riskscoring.gateway.dto.RegistrationResponse;
import com.riskscoring.gateway.dto.UserView;
import com.riskscoring.gateway.dto.VerifyEmailRequest;
import com.riskscoring.gateway.entity.AppUser;
import com.riskscoring.gateway.exception.AccountLockedException;
import com.riskscoring.gateway.exception.AccountNotActiveException;
import com.riskscoring.gateway.exception.EmailAlreadyRegisteredException;
import com.riskscoring.gateway.exception.InvalidCredentialsException;
import com.riskscoring.gateway.exception.InvalidVerificationCodeException;
import com.riskscoring.gateway.exception.UserNotFoundException;
import com.riskscoring.gateway.exception.UsernameAlreadyTakenException;
import com.riskscoring.gateway.mapper.UserMapper;
import com.riskscoring.gateway.model.UserRole;
import com.riskscoring.gateway.model.UserStatus;
import com.riskscoring.gateway.repository.AppUserRepository;
import com.riskscoring.gateway.service.AuthService;
import com.riskscoring.gateway.service.EmailVerificationService;
import com.riskscoring.gateway.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
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

    private static final String ABSENT_USER_HASH =
            "{bcrypt}$2y$12$LBfmIlKspHraK85UGm5Mgur8Tw3Ean5BHAuPpC02TvKIDOf166S5y";

    private final AppUserRepository appUserRepository;
    private final TokenService tokenService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final GatewayProperties gatewayProperties;

    @Override
    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String username = request.username().trim();

        if (appUserRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameAlreadyTakenException();
        }

        Instant now = Instant.now();
        AppUser user = appUserRepository.save(AppUser.builder()
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
                .build());

        emailVerificationService.issueAndSend(user);
        log.info("Registered user {} pending email verification", user.getId());

        return new RegistrationResponse(user.getEmail(), user.getStatus());
    }

    @Override
    @Transactional(noRollbackFor = InvalidVerificationCodeException.class)
    public IssuedSession verifyEmail(VerifyEmailRequest request, String userAgent, String ipAddress) {
        AppUser user = appUserRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(InvalidVerificationCodeException::new);

        emailVerificationService.verify(user, request.code());

        Instant now = Instant.now();
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(now);
        user.setUpdatedAt(now);

        return issueSession(user, userAgent, ipAddress);
    }

    @Override
    @Transactional
    public void resendVerificationCode(String email) {
        appUserRepository.findByEmail(normalizeEmail(email))
                .filter(user -> user.getStatus() == UserStatus.PENDING_VERIFICATION)
                .ifPresent(emailVerificationService::resend);
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
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private Optional<AppUser> findByLogin(String login) {
        String normalized = login.trim();
        return appUserRepository.findByEmail(normalized.toLowerCase(Locale.ROOT))
                .or(() -> appUserRepository.findByUsernameIgnoreCase(normalized));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
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
                gatewayProperties.auth().accessTokenTtl(),
                gatewayProperties.auth().refreshTokenTtl(),
                userMapper.toView(user)
        );
    }
}