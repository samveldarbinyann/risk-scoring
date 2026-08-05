package com.riskscoring.gateway.dto;

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
        @Size(min = 12, max = 128, message = "{validation.password.length}")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "{validation.password.weak}")
        String newPassword
) {
}
