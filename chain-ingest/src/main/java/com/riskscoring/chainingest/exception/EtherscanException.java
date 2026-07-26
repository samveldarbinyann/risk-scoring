package com.riskscoring.chainingest.exception;

public class EtherscanException extends RuntimeException {

    public EtherscanException(String message) {
        super(message);
    }

    public EtherscanException(String message, Throwable cause) {
        super(message, cause);
    }
}