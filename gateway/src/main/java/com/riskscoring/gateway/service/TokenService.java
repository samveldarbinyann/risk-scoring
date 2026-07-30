package com.riskscoring.gateway.service;

import com.riskscoring.gateway.entity.AppUser;
import com.riskscoring.gateway.security.AuthenticatedUser;

import java.util.Optional;

public interface TokenService {

    String issueAccessToken(AppUser user);

    Optional<AuthenticatedUser> resolveAccessToken(String accessToken);

    String issueRefreshToken(AppUser user, String userAgent, String ipAddress);

    AppUser consumeRefreshToken(String rawRefreshToken);

    void revokeRefreshToken(String rawRefreshToken);
}
