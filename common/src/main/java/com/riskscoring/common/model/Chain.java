package com.riskscoring.common.model;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Chain {

    ETHEREUM(ChainFamily.EVM, "Ethereum", "ETH", 18, 1, ChainSupport.SUPPORTED),
    OPTIMISM(ChainFamily.EVM, "OP Mainnet", "ETH", 18, 10, ChainSupport.SUPPORTED),
    BNB_SMART_CHAIN(ChainFamily.EVM, "BNB Smart Chain", "BNB", 18, 56, ChainSupport.SUPPORTED),
    GNOSIS(ChainFamily.EVM, "Gnosis", "xDAI", 18, 100, ChainSupport.SUPPORTED),
    POLYGON(ChainFamily.EVM, "Polygon", "POL", 18, 137, ChainSupport.SUPPORTED),
    BASE(ChainFamily.EVM, "Base", "ETH", 18, 8453, ChainSupport.SUPPORTED),
    ARBITRUM_ONE(ChainFamily.EVM, "Arbitrum One", "ETH", 18, 42161, ChainSupport.SUPPORTED),
    AVALANCHE(ChainFamily.EVM, "Avalanche C-Chain", "AVAX", 18, 43114, ChainSupport.SUPPORTED),
    LINEA(ChainFamily.EVM, "Linea", "ETH", 18, 59144, ChainSupport.SUPPORTED),

    BITCOIN(ChainFamily.BITCOIN, "Bitcoin", "BTC", 8, null, ChainSupport.SUPPORTED),
    SOLANA(ChainFamily.SOLANA, "Solana", "SOL", 9, null, ChainSupport.PLANNED),
    TRON(ChainFamily.TRON, "TRON", "TRX", 6, null, ChainSupport.PLANNED),
    TON(ChainFamily.TON, "TON", "TON", 9, null, ChainSupport.PLANNED),
    SUI(ChainFamily.SUI, "Sui", "SUI", 9, null, ChainSupport.PLANNED);

    private static final Map<String, Chain> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(Chain::name, Function.identity()));

    private static final Map<ChainFamily, List<Chain>> BY_FAMILY = Arrays.stream(values())
            .collect(Collectors.groupingBy(Chain::family, Collectors.toUnmodifiableList()));

    private final ChainFamily family;
    private final String displayName;
    private final String nativeSymbol;
    private final int nativeDecimals;
    private final BigInteger nativeUnit;
    private final Integer evmChainId;
    private final ChainSupport support;

    Chain(ChainFamily family,
          String displayName,
          String nativeSymbol,
          int nativeDecimals,
          Integer evmChainId,
          ChainSupport support) {
        this.family = family;
        this.displayName = displayName;
        this.nativeSymbol = nativeSymbol;
        this.nativeDecimals = nativeDecimals;
        this.nativeUnit = BigInteger.TEN.pow(nativeDecimals);
        this.evmChainId = evmChainId;
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

    public BigInteger nativeUnit() {
        return nativeUnit;
    }

    public Optional<Integer> evmChainId() {
        return Optional.ofNullable(evmChainId);
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

        return Optional.ofNullable(BY_NAME.get(key.trim().toUpperCase(Locale.ROOT)));
    }

    public static List<Chain> of(ChainFamily family) {
        return BY_FAMILY.getOrDefault(family, List.of());
    }

    public static String supportedKeys() {
        return Arrays.stream(values())
                .filter(Chain::scannable)
                .map(Chain::name)
                .collect(Collectors.joining(", "));
    }
}
