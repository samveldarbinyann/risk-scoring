package com.riskscoring.gateway.model;

import java.util.Locale;
import java.util.regex.Pattern;

public final class EvmAddresses {

    public static final String PATTERN = "^0x[a-fA-F0-9]{40}$";

    private static final Pattern COMPILED = Pattern.compile(PATTERN);

    private EvmAddresses() {
    }

    public static boolean isValid(String address) {
        return address != null && COMPILED.matcher(address).matches();
    }

    public static String normalize(String address) {
        return address.trim().toLowerCase(Locale.ROOT);
    }
}
