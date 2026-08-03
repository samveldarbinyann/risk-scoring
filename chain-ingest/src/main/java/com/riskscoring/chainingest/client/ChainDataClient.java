package com.riskscoring.chainingest.client;

import com.riskscoring.common.model.ChainFacts;
import com.riskscoring.common.model.ScanTarget;

public interface ChainDataClient {

    ScanTarget target();

    ChainFacts fetch(String target, int chainId);
}
