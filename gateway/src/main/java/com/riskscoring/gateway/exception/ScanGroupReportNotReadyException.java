package com.riskscoring.gateway.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ScanGroupReportNotReadyException extends RuntimeException {

    private final UUID groupId;

    public ScanGroupReportNotReadyException(UUID groupId) {
        super("Scan group %s is not completed yet".formatted(groupId));
        this.groupId = groupId;
    }
}