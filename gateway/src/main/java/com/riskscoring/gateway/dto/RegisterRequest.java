package com.riskscoring.gateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "{validation.username.required}")
        @Pattern(regexp = "^[A-Za-z0-9_]{3,32}$", message = "{validation.username.invalid}")
        String username,

        @NotBlank(message = "{validation.firstName.required}")
        @Size(max = 64, message = "{validation.firstName.tooLong}")
        String firstName,

        @NotBlank(message = "{validation.lastName.required}")
        @Size(max = 64, message = "{validation.lastName.tooLong}")
        String lastName,

        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
        @Size(max = 320, message = "{validation.email.invalid}")
        String email,

        @NotBlank(message = "{validation.password.required}")
        @Size(min = 12, max = 128, message = "{validation.password.length}")
        String password
) {
}
