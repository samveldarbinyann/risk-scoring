package com.riskscoring.gateway.dto;

public record IssuedSession(
        String accessToken,
        String refreshToken,
        UserView user
) {
}
