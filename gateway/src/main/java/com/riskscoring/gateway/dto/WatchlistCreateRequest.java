package com.riskscoring.gateway.dto;

import com.riskscoring.gateway.model.ScanTargets;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WatchlistCreateRequest(

        @NotBlank(message = "{validation.address.required}")
        @Size(max = ScanTargets.MAX_LENGTH, message = "{validation.address.invalid}")
        String address,

        @NotBlank(message = "{validation.chain.required}")
        String chain
) {
}
