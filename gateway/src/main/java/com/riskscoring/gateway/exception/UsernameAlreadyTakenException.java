package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

public class UsernameAlreadyTakenException extends ApiException {

    public UsernameAlreadyTakenException() {
        super(HttpStatus.CONFLICT, "USERNAME_ALREADY_TAKEN", "error.usernameAlreadyTaken");
    }
}
