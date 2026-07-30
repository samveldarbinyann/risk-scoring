package com.riskscoring.gateway.security;

import com.riskscoring.gateway.model.UserRole;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String username, UserRole role) {
}
