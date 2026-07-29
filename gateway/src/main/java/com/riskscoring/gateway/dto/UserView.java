package com.riskscoring.gateway.dto;

import com.riskscoring.common.model.Language;
import com.riskscoring.gateway.model.UserRole;
import com.riskscoring.gateway.model.UserStatus;

import java.util.UUID;

public record UserView(
        UUID id,
        String username,
        String firstName,
        String lastName,
        String email,
        UserRole role,
        UserStatus status,
        String avatarPath,
        Language language
) {
}