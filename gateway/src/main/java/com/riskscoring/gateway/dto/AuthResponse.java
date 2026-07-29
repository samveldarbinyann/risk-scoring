package com.riskscoring.gateway.dto;

public record AuthResponse(String accessToken, long expiresIn, UserView user) {
}