package com.riskscoring.gateway.dto;

import com.riskscoring.gateway.model.UserStatus;

public record RegistrationResponse(String email, UserStatus status) {
}