package com.riskscoring.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApiKeyRequest(
        @NotBlank(message = "{validation.apiKeyName.required}")
        @Size(max = 64, message = "{validation.apiKeyName.tooLong}")
        String name
) {
}
