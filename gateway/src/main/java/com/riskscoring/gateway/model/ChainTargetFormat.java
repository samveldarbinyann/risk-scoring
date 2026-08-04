package com.riskscoring.gateway.model;

import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.ScanTarget;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum ChainTargetFormat {

    EVM(ChainFamily.EVM,
            "^0x[a-fA-F0-9]{40}$",
            "^0x[a-fA-F0-9]{64}$",
            Normalization.LOWERCASE),

    BITCOIN(ChainFamily.BITCOIN,
            "^([13][a-km-zA-HJ-NP-Z1-9]{25,34}"
                    + "|bc1[023456789acdefghjklmnpqrstuvwxyz]{11,71}"
                    + "|BC1[023456789ACDEFGHJKLMNPQRSTUVWXYZ]{11,71})$",
            "^[a-fA-F0-9]{64}$",
            Normalization.NONE),

    SOLANA(ChainFamily.SOLANA,
            "^[1-9A-HJ-NP-Za-km-z]{43,44}$",
            "^[1-9A-HJ-NP-Za-km-z]{86,88}$",
            Normalization.NONE),

    TRON(ChainFamily.TRON,
            "^T[1-9A-HJ-NP-Za-km-z]{33}$",
            "^[a-fA-F0-9]{64}$",
            Normalization.NONE),

    TON(ChainFamily.TON,
            "^(0:[a-fA-F0-9]{64}|[A-Za-z0-9_-]{48})$",
            "^([A-Za-z0-9_-]{44}|[a-fA-F0-9]{64})$",
            Normalization.NONE),

    SUI(ChainFamily.SUI,
            "^0x[a-fA-F0-9]{64}$",
            "^[1-9A-HJ-NP-Za-km-z]{43,44}$",
            Normalization.LOWERCASE);

    private enum Normalization {
        LOWERCASE,
        NONE
    }

    private static final Map<ChainFamily, ChainTargetFormat> BY_FAMILY = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ChainTargetFormat::family, Function.identity()));

    private final ChainFamily family;
    private final Pattern addressPattern;
    private final Pattern transactionPattern;
    private final Normalization normalization;

    ChainTargetFormat(ChainFamily family, String addressPattern, String transactionPattern, Normalization normalization) {
        this.family = family;
        this.addressPattern = Pattern.compile(addressPattern);
        this.transactionPattern = Pattern.compile(transactionPattern);
        this.normalization = normalization;
    }

    public ChainFamily family() {
        return family;
    }

    public String normalize(String value) {
        String trimmed = value == null ? "" : value.trim();
        return normalization == Normalization.LOWERCASE ? trimmed.toLowerCase(Locale.ROOT) : trimmed;
    }

    public Optional<ScanTarget> classify(String value) {
        String normalized = normalize(value);

        if (addressPattern.matcher(normalized).matches()) {
            return Optional.of(ScanTarget.ADDRESS);
        }
        if (transactionPattern.matcher(normalized).matches()) {
            return Optional.of(ScanTarget.TRANSACTION);
        }

        return Optional.empty();
    }

    public static ChainTargetFormat of(ChainFamily family) {
        ChainTargetFormat format = BY_FAMILY.get(family);
        if (format == null) {
            throw new IllegalStateException("No target format for family " + family);
        }
        return format;
    }
}
