package com.riskscoring.gateway.exception;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Refresh token is missing, expired or already used");
    }
}