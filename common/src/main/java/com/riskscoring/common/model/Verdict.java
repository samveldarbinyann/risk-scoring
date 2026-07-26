package com.riskscoring.common.model;

import java.util.List;

public record Verdict(
        RiskLevel riskLevel,
        int score,
        String explanation,
        List<String> decisiveSignals,
        List<String> manualChecks
) {
}
