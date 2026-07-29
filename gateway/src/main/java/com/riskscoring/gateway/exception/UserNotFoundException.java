package com.riskscoring.gateway.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class UserNotFoundException extends RuntimeException {

    private final UUID userId;

    public UserNotFoundException(UUID userId) {
        super("User not found: %s".formatted(userId));
        this.userId = userId;
    }
}