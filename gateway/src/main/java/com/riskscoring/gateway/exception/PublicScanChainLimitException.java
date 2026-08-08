package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class PublicScanChainLimitException extends ApiException {

    public PublicScanChainLimitException(int requested, int allowed) {
        super(HttpStatus.UNAUTHORIZED, "PUBLIC_SCAN_CHAIN_LIMIT", "error.publicScanChainLimit", requested, allowed);
    }
}
