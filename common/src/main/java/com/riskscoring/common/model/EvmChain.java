package com.riskscoring.common.model;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public enum EvmChain {

    ETHEREUM(1, "Ethereum"),
    POLYGON(137, "Polygon"),
    ARBITRUM_ONE(42161, "Arbitrum One"),
    LINEA(59144, "Linea"),
    GNOSIS(100, "Gnosis"),
    SEPOLIA(11155111, "Sepolia");

    private final int chainId;
    private final String displayName;

    EvmChain(int chainId, String displayName) {
        this.chainId = chainId;
        this.displayName = displayName;
    }

    public int chainId() {
        return chainId;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<EvmChain> byId(int chainId) {
        return Arrays.stream(values())
                .filter(chain -> chain.chainId == chainId)
                .findFirst();
    }

    public static String supportedIds() {
        return Arrays.stream(values())
                .map(chain -> "%d (%s)".formatted(chain.chainId, chain.displayName))
                .collect(Collectors.joining(", "));
    }
}