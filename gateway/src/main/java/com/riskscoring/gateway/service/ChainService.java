package com.riskscoring.gateway.service;

import com.riskscoring.common.model.Chain;
import com.riskscoring.gateway.dto.ChainCandidatesResponse;
import com.riskscoring.gateway.dto.ChainView;

import java.util.List;

public interface ChainService {

    List<ChainView> registry();

    ChainCandidatesResponse candidatesFor(String target);

    Chain requireScannable(String key);
}
