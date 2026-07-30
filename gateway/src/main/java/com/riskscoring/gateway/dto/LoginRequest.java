package com.riskscoring.gateway.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "{validation.login.required}") String login,
        @NotBlank(message = "{validation.password.required}") String password
) {
}
