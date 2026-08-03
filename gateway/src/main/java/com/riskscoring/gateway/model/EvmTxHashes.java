package com.riskscoring.gateway.model;

import java.util.regex.Pattern;

public final class EvmTxHashes {

    public static final String PATTERN = "^0x[a-fA-F0-9]{64}$";

    private static final Pattern COMPILED = Pattern.compile(PATTERN);

    private EvmTxHashes() {
    }

    public static boolean isValid(String hash) {
        return hash != null && COMPILED.matcher(hash).matches();
    }
}
