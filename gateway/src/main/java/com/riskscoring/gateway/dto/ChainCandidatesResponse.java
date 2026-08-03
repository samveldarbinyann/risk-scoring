package com.riskscoring.gateway.dto;

import java.util.List;

public record ChainCandidatesResponse(
        String target,
        List<ChainCandidate> candidates
) {
}
