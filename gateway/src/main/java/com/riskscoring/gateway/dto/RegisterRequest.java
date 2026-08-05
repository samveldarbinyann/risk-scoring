package com.riskscoring.gateway.dto;

import com.riskscoring.gateway.validation.PasswordPolicy;
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
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH, message = "{validation.password.length}")
        @Pattern(regexp = PasswordPolicy.PATTERN, message = "{validation.password.weak}")
        String password
) {
}
