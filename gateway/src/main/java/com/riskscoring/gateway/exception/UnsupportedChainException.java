package com.riskscoring.gateway.exception;

import com.riskscoring.common.model.EvmChain;
import org.springframework.http.HttpStatus;

public class UnsupportedChainException extends ApiException {

    public UnsupportedChainException(int chainId) {
        super(HttpStatus.BAD_REQUEST, "UNSUPPORTED_CHAIN", "error.unsupportedChain", chainId, EvmChain.supportedIds());
    }
}
