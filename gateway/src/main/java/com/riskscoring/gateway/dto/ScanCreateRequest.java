package com.riskscoring.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record ScanCreateRequest(

        @NotBlank(message = "{validation.address.required}")
        @Pattern(regexp = EVM_ADDRESS_PATTERN, message = "{validation.address.invalid}")
        String address,

        @Positive(message = "{validation.chainId.positive}")
        int chainId
) {
    public static final String EVM_ADDRESS_PATTERN = "^0x[a-fA-F0-9]{40}$";
}