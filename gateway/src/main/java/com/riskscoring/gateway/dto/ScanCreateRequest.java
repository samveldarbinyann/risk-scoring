package com.riskscoring.gateway.dto;

import com.riskscoring.gateway.model.AddressPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record ScanCreateRequest(

        @NotBlank(message = "{validation.address.required}")
        @Pattern(regexp = AddressPatterns.EVM_ADDRESS, message = "{validation.address.invalid}")
        String address,

        List<Integer> chainIds
) {
}
