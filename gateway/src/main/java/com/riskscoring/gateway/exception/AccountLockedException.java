package com.riskscoring.gateway.exception;

import lombok.Getter;

import java.time.Instant;

@Getter
public class AccountLockedException extends RuntimeException {

    private final Instant lockedUntil;

    public AccountLockedException(Instant lockedUntil) {
        super("Account is locked until %s".formatted(lockedUntil));
        this.lockedUntil = lockedUntil;
    }
}