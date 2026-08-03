package com.riskscoring.gateway.dto;

import com.riskscoring.gateway.model.ScanTargets;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ScanCreateRequest(

        @NotBlank(message = "{validation.target.required}")
        @Size(max = ScanTargets.MAX_LENGTH, message = "{validation.target.tooLong}")
        String target,

        List<String> chains
) {
}
