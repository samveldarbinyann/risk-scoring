package com.riskscoring.gateway.exception;

import com.riskscoring.gateway.model.UserStatus;
import lombok.Getter;

@Getter
public class AccountNotActiveException extends RuntimeException {

    private final UserStatus status;

    public AccountNotActiveException(UserStatus status) {
        super("Account is not active, current status: %s".formatted(status));
        this.status = status;
    }
}