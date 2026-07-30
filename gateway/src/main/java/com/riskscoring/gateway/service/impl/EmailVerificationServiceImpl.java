package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.entity.AppUser;
import com.riskscoring.gateway.entity.EmailVerificationCode;
import com.riskscoring.gateway.exception.InvalidVerificationCodeException;
import com.riskscoring.gateway.exception.ResendCooldownException;
import com.riskscoring.gateway.exception.TooManyVerificationAttemptsException;
import com.riskscoring.gateway.exception.VerificationCodeExpiredException;
import com.riskscoring.gateway.model.EmailCodePurpose;
import com.riskscoring.gateway.repository.EmailVerificationCodeRepository;
import com.riskscoring.gateway.security.SecretHasher;
import com.riskscoring.gateway.service.EmailService;
import com.riskscoring.gateway.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final int CODE_BOUND = 1_000_000;
    private static final String CODE_FORMAT = "%06d";

    private final EmailVerificationCodeRepository codeRepository;
    private final EmailService emailService;
    private final SecretHasher secretHasher;
    private final GatewayProperties gatewayProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void issueAndSend(AppUser user) {
        String code = generateCode();
        persist(user, code);
        emailService.sendVerificationCode(user, code);
    }

    @Override
    @Transactional
    public void resend(AppUser user) {
        enforceCooldown(user);
        issueAndSend(user);
    }

    @Override
    @Transactional(noRollbackFor = {
            InvalidVerificationCodeException.class,
            TooManyVerificationAttemptsException.class
    })
    public void verify(AppUser user, String code) {
        EmailVerificationCode stored = activeCode(user)
                .orElseThrow(InvalidVerificationCodeException::new);

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new VerificationCodeExpiredException();
        }

        if (stored.getAttempts() >= maxAttempts()) {
            throw new TooManyVerificationAttemptsException();
        }

        if (!secretHasher.matches(code, stored.getCodeHash())) {
            if (codeRepository.incrementAttempts(stored.getId(), maxAttempts()) == 0) {
                throw new TooManyVerificationAttemptsException();
            }
            log.warn("Failed verification attempt for user {}", user.getId());
            throw new InvalidVerificationCodeException();
        }

        if (codeRepository.markUsed(stored.getId(), Instant.now()) == 0) {
            throw new InvalidVerificationCodeException();
        }
    }

    private Optional<EmailVerificationCode> activeCode(AppUser user) {
        return codeRepository.findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                user.getId(), EmailCodePurpose.REGISTRATION);
    }

    private void enforceCooldown(AppUser user) {
        activeCode(user).ifPresent(code -> {
            Instant readyAt = code.getCreatedAt().plus(cooldown());
            if (readyAt.isAfter(Instant.now())) {
                throw new ResendCooldownException(Duration.between(Instant.now(), readyAt).toSeconds() + 1);
            }
        });
    }

    private void persist(AppUser user, String code) {
        activeCode(user).ifPresent(previous -> codeRepository.markUsed(previous.getId(), Instant.now()));

        Instant now = Instant.now();
        codeRepository.save(EmailVerificationCode.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .codeHash(secretHasher.hash(code))
                .purpose(EmailCodePurpose.REGISTRATION)
                .expiresAt(now.plus(gatewayProperties.verification().codeTtl()))
                .attempts(0)
                .createdAt(now)
                .build());
    }

    private String generateCode() {
        return CODE_FORMAT.formatted(secureRandom.nextInt(CODE_BOUND));
    }

    private int maxAttempts() {
        return gatewayProperties.verification().maxAttempts();
    }

    private Duration cooldown() {
        return gatewayProperties.verification().resendCooldown();
    }
}
