package com.riskscoring.riskai.exception;

import com.riskscoring.common.model.ChainFamily;

public class UnsupportedChainFamilyException extends RuntimeException {

    public UnsupportedChainFamilyException(ChainFamily family) {
        super("No prompt rules defined for chain family %s".formatted(family));
    }
}
