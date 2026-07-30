package com.riskscoring.gateway.dto;

import com.riskscoring.gateway.model.EvmAddresses;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record ScanCreateRequest(

        @NotBlank(message = "{validation.address.required}")
        @Pattern(regexp = EvmAddresses.PATTERN, message = "{validation.address.invalid}")
        String address,

        List<Integer> chainIds
) {
}
