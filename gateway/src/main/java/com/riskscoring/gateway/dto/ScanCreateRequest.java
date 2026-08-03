package com.riskscoring.gateway.dto;

import com.riskscoring.gateway.model.ScanTargets;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record ScanCreateRequest(

        @NotBlank(message = "{validation.target.required}")
        @Pattern(regexp = ScanTargets.PATTERN, message = "{validation.target.invalid}")
        String target,

        List<Integer> chainIds
) {
}
