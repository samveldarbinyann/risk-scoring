package com.riskscoring.gateway.dto;

import com.riskscoring.gateway.model.AddressFamily;

import java.util.List;

public record ChainCandidatesResponse(
        String address,
        AddressFamily family,
        List<ChainCandidateView> chains
) {
}
