package com.riskscoring.common.model;

public record FlaggedExposure(
        String address,
        LabelCategory category,
        String label,
        String source,
        TransferDirection direction,
        int hops,
        String valueNative
) {
}
