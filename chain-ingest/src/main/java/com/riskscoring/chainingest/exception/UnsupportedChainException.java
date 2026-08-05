package com.riskscoring.chainingest.exception;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ScanTarget;

import java.util.List;
import java.util.Locale;

public class UnsupportedChainException extends RuntimeException implements UserFacingChainFailure {

    private final String messageKey;
    private final List<Object> messageArgs;

    public UnsupportedChainException(Chain chain, ScanTarget target) {
        super("%s %s scans are not supported yet"
                .formatted(chain.displayName(), target.name().toLowerCase(Locale.ROOT)));
        this.messageKey = target == ScanTarget.ADDRESS
                ? "console.message.unsupportedChainAddress"
                : "console.message.unsupportedChainTransaction";
        this.messageArgs = List.of(chain.displayName());
    }

    @Override
    public String progressMessageKey() {
        return messageKey;
    }

    @Override
    public List<Object> progressMessageArgs() {
        return messageArgs;
    }
}
