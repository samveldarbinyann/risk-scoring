package com.riskscoring.chainingest.exception;

public class ChainDataException extends RuntimeException {

    public ChainDataException(String message) {
        super(message);
    }

    public ChainDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
