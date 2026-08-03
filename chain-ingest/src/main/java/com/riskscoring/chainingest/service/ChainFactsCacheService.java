package com.riskscoring.chainingest.service;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFacts;
import com.riskscoring.common.model.ScanTarget;

import java.util.Optional;

public interface ChainFactsCacheService {

    ScanTarget target();

    Optional<ChainFacts> findFresh(String subject, Chain chain);

    void store(String subject, Chain chain, ChainFacts facts);
}
