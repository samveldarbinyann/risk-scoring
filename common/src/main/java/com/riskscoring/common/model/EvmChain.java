package com.riskscoring.common.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum EvmChain {

    ETHEREUM(1, "Ethereum", true),
    OPTIMISM(10, "OP Mainnet", true),
    BNB_SMART_CHAIN(56, "BNB Smart Chain", true),
    GNOSIS(100, "Gnosis", true),
    POLYGON(137, "Polygon", true),
    BASE(8453, "Base", true),
    ARBITRUM_ONE(42161, "Arbitrum One", true),
    AVALANCHE(43114, "Avalanche C-Chain", true),
    LINEA(59144, "Linea", true),
    SEPOLIA(11155111, "Sepolia", false);

    private final int chainId;
    private final String displayName;
    private final boolean mainnet;

    EvmChain(int chainId, String displayName, boolean mainnet) {
        this.chainId = chainId;
        this.displayName = displayName;
        this.mainnet = mainnet;
    }

    public int chainId() {
        return chainId;
    }

    public String displayName() {
        return displayName;
    }

    public boolean mainnet() {
        return mainnet;
    }

    public static Optional<EvmChain> byId(int chainId) {
        return Arrays.stream(values())
                .filter(chain -> chain.chainId == chainId)
                .findFirst();
    }

    public static List<EvmChain> mainnets() {
        return Arrays.stream(values())
                .filter(EvmChain::mainnet)
                .toList();
    }

    public static String supportedIds() {
        return Arrays.stream(values())
                .map(chain -> "%d (%s)".formatted(chain.chainId, chain.displayName))
                .collect(Collectors.joining(", "));
    }
}
