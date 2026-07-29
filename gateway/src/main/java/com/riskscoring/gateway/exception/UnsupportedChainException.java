package com.riskscoring.gateway.exception;

import lombok.Getter;

@Getter
public class UnsupportedChainException extends RuntimeException {

    private final int chainId;

    public UnsupportedChainException(int chainId) {
        super("Unsupported chain: " + chainId);
        this.chainId = chainId;
    }
}
