package com.riskscoring.enrichment.mapper;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFamily;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class OfacChainMapping {

    private static final Set<String> EVM_TICKERS = Set.of("ETH", "BSC", "ARB");

    private static final Map<String, Chain> DIRECT_TICKERS = Map.of(
            "XBT", Chain.BITCOIN,
            "TRX", Chain.TRON,
            "SOL", Chain.SOLANA
    );

    private static final List<Chain> SCANNABLE_EVM_CHAINS = Chain.of(ChainFamily.EVM).stream()
            .filter(Chain::scannable)
            .toList();

    public List<Chain> chainsFor(String ticker) {
        if (EVM_TICKERS.contains(ticker)) {
            return SCANNABLE_EVM_CHAINS;
        }

        return Optional.ofNullable(DIRECT_TICKERS.get(ticker))
                .map(List::of)
                .orElse(List.of());
    }
}
