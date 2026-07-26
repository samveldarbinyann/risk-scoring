package com.riskscoring.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record ScanCreateRequest(

        @NotBlank(message = "address is required")
        @Pattern(regexp = EVM_ADDRESS_PATTERN, message = "must be a valid EVM address (0x + 40 hex chars)")
        String address,

        @Positive(message = "chainId must be positive")
        int chainId
) {
    public static final String EVM_ADDRESS_PATTERN = "^0x[a-fA-F0-9]{40}$";
}