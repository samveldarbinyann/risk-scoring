package com.riskscoring.chainingest.service;

import com.riskscoring.common.model.ChainFacts;
import com.riskscoring.common.model.ScanTarget;

import java.util.Optional;

public interface ChainFactsCacheService {

    ScanTarget target();

    Optional<ChainFacts> findFresh(String subject, int chainId);

    void store(String subject, int chainId, ChainFacts facts);
}
