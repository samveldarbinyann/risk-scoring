package com.riskscoring.gateway.dto;

import com.riskscoring.gateway.validation.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
        String email,

        @NotBlank(message = "{validation.code.required}")
        @Pattern(regexp = "^\\d{6}$", message = "{validation.code.invalid}")
        String code,

        @NotBlank(message = "{validation.password.required}")
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH, message = "{validation.password.length}")
        @Pattern(regexp = PasswordPolicy.PATTERN, message = "{validation.password.weak}")
        String newPassword
) {
}
