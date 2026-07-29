package com.riskscoring.gateway.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public enum AddressFamily {

    EVM(AddressPatterns.EVM_ADDRESS);

    private final Pattern pattern;

    AddressFamily(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    public boolean matches(String address) {
        return address != null && pattern.matcher(address).matches();
    }

    public String normalize(String address) {
        return switch (this) {
            case EVM -> address.trim().toLowerCase(Locale.ROOT);
        };
    }

    public static Optional<AddressFamily> detect(String address) {
        return Arrays.stream(values())
                .filter(family -> family.matches(address))
                .findFirst();
    }
}
