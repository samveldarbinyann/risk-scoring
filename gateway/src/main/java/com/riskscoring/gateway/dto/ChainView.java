package com.riskscoring.gateway.dto;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.ChainSupport;

public record ChainView(
        Chain chain,
        ChainFamily family,
        String displayName,
        String nativeSymbol,
        int nativeDecimals,
        Integer evmChainId,
        boolean mainnet,
        ChainSupport support
) {
}
