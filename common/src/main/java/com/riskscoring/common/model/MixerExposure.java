package com.riskscoring.common.model;

import java.util.List;

public record MixerExposure(
        List<String> services,
        int percentOfVolume,
        String valueNative
) {
}
