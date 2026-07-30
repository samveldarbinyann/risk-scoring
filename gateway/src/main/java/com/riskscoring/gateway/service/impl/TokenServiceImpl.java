package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.entity.AppUser;
import com.riskscoring.gateway.entity.RefreshToken;
import com.riskscoring.gateway.exception.InvalidRefreshTokenException;
import com.riskscoring.gateway.model.UserStatus;
import com.riskscoring.gateway.repository.AppUserRepository;
import com.riskscoring.gateway.repository.RefreshTokenRepository;
import com.riskscoring.gateway.security.AuthenticatedUser;
import com.riskscoring.gateway.security.SecretHasher;
import com.riskscoring.gateway.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String TOKEN_VERSION_CLAIM = "ver";
    private static final String ROLE_CLAIM = "role";
    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final int USER_AGENT_MAX_LENGTH = 255;
    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecretHasher secretHasher;
    private final GatewayProperties gatewayProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String issueAccessToken(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(auth().accessTokenTtl()))
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .claim(TOKEN_VERSION_CLAIM, user.getTokenVersion())
                .claim(ROLE_CLAIM, user.getRole().name())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> resolveAccessToken(String accessToken) {
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(accessToken);
        } catch (JwtException exception) {
            return Optional.empty();
        }

        if (!ACCESS_TOKEN_TYPE.equals(jwt.getClaimAsString(TOKEN_TYPE_CLAIM))) {
            return Optional.empty();
        }

        Number tokenVersion = jwt.getClaim(TOKEN_VERSION_CLAIM);
        if (tokenVersion == null) {
            return Optional.empty();
        }

        return appUserRepository.findById(UUID.fromString(jwt.getSubject()))
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .filter(user -> user.getTokenVersion() == tokenVersion.intValue())
                .map(user -> new AuthenticatedUser(user.getId(), user.getUsername(), user.getRole()));
    }

    @Override
    @Transactional
    public String issueRefreshToken(AppUser user, String userAgent, String ipAddress) {
        byte[] raw = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(raw);
        String token = TOKEN_ENCODER.encodeToString(raw);

        Instant now = Instant.now();
        refreshTokenRepository.save(RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash(secretHasher.hash(token))
                .issuedAt(now)
                .expiresAt(now.plus(auth().refreshTokenTtl()))
                .userAgent(truncate(userAgent))
                .ipAddress(ipAddress)
                .build());

        return token;
    }

    @Override
    @Transactional
    public AppUser consumeRefreshToken(String rawRefreshToken) {
        RefreshToken token = activeToken(rawRefreshToken)
                .orElseThrow(InvalidRefreshTokenException::new);

        token.setRevokedAt(Instant.now());

        return appUserRepository.findById(token.getUserId())
                .orElseThrow(InvalidRefreshTokenException::new);
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        activeToken(rawRefreshToken).ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    private Optional<RefreshToken> activeToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return Optional.empty();
        }
        return refreshTokenRepository.findByTokenHash(secretHasher.hash(rawRefreshToken))
                .filter(token -> token.getRevokedAt() == null)
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()));
    }

    private String truncate(String userAgent) {
        if (userAgent == null || userAgent.length() <= USER_AGENT_MAX_LENGTH) {
            return userAgent;
        }
        return userAgent.substring(0, USER_AGENT_MAX_LENGTH);
    }

    private GatewayProperties.Auth auth() {
        return gatewayProperties.auth();
    }
}
