package com.riskscoring.gateway.dto;

import com.riskscoring.gateway.model.EvmAddresses;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record WatchlistCreateRequest(

        @NotBlank(message = "{validation.address.required}")
        @Pattern(regexp = EvmAddresses.PATTERN, message = "{validation.address.invalid}")
        String address,

        @NotNull(message = "{validation.chainId.required}")
        Integer chainId
) {
}
