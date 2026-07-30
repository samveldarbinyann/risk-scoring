package com.riskscoring.gateway.exception;

import com.riskscoring.gateway.model.UserStatus;
import org.springframework.http.HttpStatus;

public class AccountNotActiveException extends ApiException {

    public AccountNotActiveException(UserStatus status) {
        super(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", "error.accountNotActive", status);
    }
}
