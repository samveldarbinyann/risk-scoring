package com.riskscoring.gateway.exception;

import lombok.Getter;

@Getter
public class UnrecognizedAddressException extends RuntimeException {

    private final String address;

    public UnrecognizedAddressException(String address) {
        super("Unrecognized address format: " + address);
        this.address = address;
    }
}
