package com.riskscoring.gateway.exception;

import com.riskscoring.common.model.Chain;
import org.springframework.http.HttpStatus;

public class ChainNotSupportedYetException extends ApiException {

    public ChainNotSupportedYetException(Chain chain) {
        super(HttpStatus.BAD_REQUEST, "CHAIN_NOT_SUPPORTED_YET", "error.chainNotSupportedYet", chain.displayName());
    }
}
