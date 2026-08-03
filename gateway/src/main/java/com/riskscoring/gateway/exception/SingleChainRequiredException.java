package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class SingleChainRequiredException extends ApiException {

    public SingleChainRequiredException(int requested) {
        super(HttpStatus.BAD_REQUEST, "SINGLE_CHAIN_REQUIRED", "error.singleChainRequired", requested);
    }
}
