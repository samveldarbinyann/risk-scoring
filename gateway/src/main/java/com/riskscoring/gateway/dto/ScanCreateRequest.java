package com.riskscoring.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record ScanCreateRequest(

        @NotBlank(message = "{validation.address.required}")
        @Pattern(regexp = EVM_ADDRESS_PATTERN, message = "{validation.address.invalid}")
        String address,

        List<Integer> chainIds
) {
    public static final String EVM_ADDRESS_PATTERN = "^0x[a-fA-F0-9]{40}$";
}
