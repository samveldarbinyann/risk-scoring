package com.riskscoring.gateway.exception;

import com.riskscoring.common.model.Chain;
import org.springframework.http.HttpStatus;

public class TargetChainMismatchException extends ApiException {

    public TargetChainMismatchException(String target, Chain chain) {
        super(HttpStatus.BAD_REQUEST, "TARGET_CHAIN_MISMATCH", "error.targetChainMismatch", target, chain.displayName());
    }
}
