package com.riskscoring.gateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ContactRequest(
        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
        @Size(max = 320, message = "{validation.email.tooLong}")
        String email,

        @NotBlank(message = "{validation.subject.required}")
        @Size(max = 255, message = "{validation.subject.tooLong}")
        String subject,

        @NotBlank(message = "{validation.message.required}")
        @Size(max = 5000, message = "{validation.message.tooLong}")
        String message,

        UUID scanId
) {
}
