package com.riskscoring.chainingest.client;

import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.ScanTarget;

public record ChainDataClientKey(ChainFamily family, ScanTarget target) {

    public static ChainDataClientKey of(ChainDataClient client) {
        return new ChainDataClientKey(client.family(), client.target());
    }
}
