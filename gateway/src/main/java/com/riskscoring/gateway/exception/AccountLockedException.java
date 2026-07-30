package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public class AccountLockedException extends ApiException {

    public AccountLockedException(Instant lockedUntil) {
        super(HttpStatus.LOCKED, "ACCOUNT_LOCKED", "error.accountLocked", lockedUntil);
    }
}
