package com.riskscoring.gateway.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ScanGroupNotFoundException extends RuntimeException {

    private final UUID groupId;

    public ScanGroupNotFoundException(UUID groupId) {
        super("Scan group not found: " + groupId);
        this.groupId = groupId;
    }
}