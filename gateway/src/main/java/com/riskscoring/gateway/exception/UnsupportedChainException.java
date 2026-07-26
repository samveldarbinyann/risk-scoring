package com.riskscoring.gateway.exception;

import com.riskscoring.common.model.EvmChain;

public class UnsupportedChainException extends RuntimeException {

    public UnsupportedChainException(int chainId) {
        super("Unsupported chainId %d, supported: %s".formatted(chainId, EvmChain.supportedIds()));
    }
}
