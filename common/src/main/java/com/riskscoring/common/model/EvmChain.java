package com.riskscoring.common.model;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public enum EvmChain {

    ETHEREUM(1, "Ethereum"),
    OPTIMISM(10, "OP Mainnet"),
    BNB_SMART_CHAIN(56, "BNB Smart Chain"),
    GNOSIS(100, "Gnosis"),
    POLYGON(137, "Polygon"),
    BASE(8453, "Base"),
    ARBITRUM_ONE(42161, "Arbitrum One"),
    AVALANCHE(43114, "Avalanche C-Chain"),
    LINEA(59144, "Linea"),
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