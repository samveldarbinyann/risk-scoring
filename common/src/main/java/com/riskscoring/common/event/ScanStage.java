package com.riskscoring.common.event;

public enum ScanStage {
    PENDING,
    FETCHING,
    ENRICHING,
    ANALYZING,
    COMPLETED,
    FAILED
}