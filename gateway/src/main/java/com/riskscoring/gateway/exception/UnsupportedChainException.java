package com.riskscoring.gateway.exception;

import com.riskscoring.common.model.Chain;
import org.springframework.http.HttpStatus;

public class UnsupportedChainException extends ApiException {

    public UnsupportedChainException(String key) {
        super(HttpStatus.BAD_REQUEST, "UNSUPPORTED_CHAIN", "error.unsupportedChain", key, Chain.supportedKeys());
    }
}
