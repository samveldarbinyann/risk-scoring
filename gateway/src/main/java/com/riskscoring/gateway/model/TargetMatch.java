package com.riskscoring.gateway.model;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ScanTarget;

public record TargetMatch(Chain chain, ScanTarget targetType, String normalizedTarget) {
}
