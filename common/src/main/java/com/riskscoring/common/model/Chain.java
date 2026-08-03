package com.riskscoring.common.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

public enum Chain {

    ETHEREUM(ChainFamily.EVM, "Ethereum", "ETH", 18, 1, true, ChainSupport.SUPPORTED),
    OPTIMISM(ChainFamily.EVM, "OP Mainnet", "ETH", 18, 10, true, ChainSupport.SUPPORTED),
    BNB_SMART_CHAIN(ChainFamily.EVM, "BNB Smart Chain", "BNB", 18, 56, true, ChainSupport.SUPPORTED),
    GNOSIS(ChainFamily.EVM, "Gnosis", "xDAI", 18, 100, true, ChainSupport.SUPPORTED),
    POLYGON(ChainFamily.EVM, "Polygon", "POL", 18, 137, true, ChainSupport.SUPPORTED),
    BASE(ChainFamily.EVM, "Base", "ETH", 18, 8453, true, ChainSupport.SUPPORTED),
    ARBITRUM_ONE(ChainFamily.EVM, "Arbitrum One", "ETH", 18, 42161, true, ChainSupport.SUPPORTED),
    AVALANCHE(ChainFamily.EVM, "Avalanche C-Chain", "AVAX", 18, 43114, true, ChainSupport.SUPPORTED),
    LINEA(ChainFamily.EVM, "Linea", "ETH", 18, 59144, true, ChainSupport.SUPPORTED),
    SEPOLIA(ChainFamily.EVM, "Sepolia", "ETH", 18, 11155111, false, ChainSupport.SUPPORTED),

    BITCOIN(ChainFamily.BITCOIN, "Bitcoin", "BTC", 8, null, true, ChainSupport.SUPPORTED),
    SOLANA(ChainFamily.SOLANA, "Solana", "SOL", 9, null, true, ChainSupport.PLANNED),
    TRON(ChainFamily.TRON, "TRON", "TRX", 6, null, true, ChainSupport.PLANNED),
    TON(ChainFamily.TON, "TON", "TON", 9, null, true, ChainSupport.PLANNED),
    SUI(ChainFamily.SUI, "Sui", "SUI", 9, null, true, ChainSupport.PLANNED);

    private final ChainFamily family;
    private final String displayName;
    private final String nativeSymbol;
    private final int nativeDecimals;
    private final Integer evmChainId;
    private final boolean mainnet;
    private final ChainSupport support;

    Chain(ChainFamily family,
          String displayName,
          String nativeSymbol,
          int nativeDecimals,
          Integer evmChainId,
          boolean mainnet,
          ChainSupport support) {
        this.family = family;
        this.displayName = displayName;
        this.nativeSymbol = nativeSymbol;
        this.nativeDecimals = nativeDecimals;
        this.evmChainId = evmChainId;
        this.mainnet = mainnet;
        this.support = support;
    }

    public ChainFamily family() {
        return family;
    }

    public String displayName() {
        return displayName;
    }

    public String nativeSymbol() {
        return nativeSymbol;
    }

    public int nativeDecimals() {
        return nativeDecimals;
    }

    public OptionalInt evmChainId() {
        return evmChainId == null ? OptionalInt.empty() : OptionalInt.of(evmChainId);
    }

    public boolean mainnet() {
        return mainnet;
    }

    public ChainSupport support() {
        return support;
    }

    public boolean scannable() {
        return support == ChainSupport.SUPPORTED;
    }

    public static Optional<Chain> byKey(String key) {
        if (key == null) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(chain -> chain.name().equals(key.trim().toUpperCase(Locale.ROOT)))
                .findFirst();
    }

    public static List<Chain> of(ChainFamily family) {
        return Arrays.stream(values())
                .filter(chain -> chain.family == family)
                .toList();
    }

    public static List<Chain> scannableMainnets() {
        return Arrays.stream(values())
                .filter(chain -> chain.mainnet && chain.scannable())
                .toList();
    }

    public static String supportedKeys() {
        return Arrays.stream(values())
                .filter(Chain::scannable)
                .map(Chain::name)
                .collect(Collectors.joining(", "));
    }
}
