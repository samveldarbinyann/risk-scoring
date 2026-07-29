package com.riskscoring.gateway.dto;

import java.time.Duration;

public record IssuedSession(
        String accessToken,
        String refreshToken,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        UserView user
) {
}