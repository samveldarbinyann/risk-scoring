package com.riskscoring.chainingest.client;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFacts;
import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.ScanTarget;

public interface ChainDataClient {

    ChainFamily family();

    ScanTarget target();

    ChainFacts fetch(String target, Chain chain);
}
