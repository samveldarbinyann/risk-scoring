package com.riskscoring.gateway.service;

import com.riskscoring.gateway.dto.ChainCandidatesResponse;

public interface ChainService {

    ChainCandidatesResponse candidatesFor(String address);
}
