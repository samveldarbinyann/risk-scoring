package com.riskscoring.chainingest.exception;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ScanTarget;

import java.util.Locale;

public class UnsupportedChainException extends RuntimeException implements UserFacingChainFailure {

    public UnsupportedChainException(Chain chain, ScanTarget target) {
        super("%s %s scans are not supported yet"
                .formatted(chain.displayName(), target.name().toLowerCase(Locale.ROOT)));
    }

    @Override
    public String progressMessage() {
        return getMessage();
    }
}
