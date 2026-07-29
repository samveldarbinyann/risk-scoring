package com.riskscoring.chainingest.exception;

public class MoralisException extends RuntimeException {

    public MoralisException(String message) {
        super(message);
    }

    public MoralisException(String message, Throwable cause) {
        super(message, cause);
    }
}
