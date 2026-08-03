package com.riskscoring.gateway.model;

import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.gateway.exception.UnrecognizedTargetException;

import java.util.Locale;

public final class ScanTargets {

    public static final String PATTERN = "^0x([a-fA-F0-9]{40}|[a-fA-F0-9]{64})$";

    private ScanTargets() {
    }

    public static ScanTarget classify(String value) {
        String normalized = normalize(value);

        if (EvmAddresses.isValid(normalized)) {
            return ScanTarget.ADDRESS;
        }
        if (EvmTxHashes.isValid(normalized)) {
            return ScanTarget.TRANSACTION;
        }

        throw new UnrecognizedTargetException(value);
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
