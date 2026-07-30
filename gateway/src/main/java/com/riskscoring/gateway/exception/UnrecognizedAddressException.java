package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class UnrecognizedAddressException extends ApiException {

    public UnrecognizedAddressException(String address) {
        super(HttpStatus.BAD_REQUEST, "UNRECOGNIZED_ADDRESS", "error.unrecognizedAddress", address);
    }
}
