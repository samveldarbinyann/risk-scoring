package com.riskscoring.gateway.exception;

import com.riskscoring.common.model.EvmChain;
import lombok.Getter;

@Getter
public class UnsupportedChainException extends RuntimeException {

    private final int chainId;

    public UnsupportedChainException(int chainId) {
        super("Unsupported chainId %d, supported: %s".formatted(chainId, EvmChain.supportedIds()));
        this.chainId = chainId;
    }
}