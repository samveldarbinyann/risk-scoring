package com.riskscoring.enrichment.mapper;

import com.riskscoring.common.model.Chain;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OfacChainMappingTest {

    private final OfacChainMapping mapping = new OfacChainMapping();

    @Test
    void evmTickerReplicatesAcrossEveryScannableEvmChain() {
        List<Chain> chains = mapping.chainsFor("ETH");

        assertThat(chains).containsExactlyInAnyOrder(
                Chain.ETHEREUM, Chain.OPTIMISM, Chain.BNB_SMART_CHAIN, Chain.GNOSIS,
                Chain.POLYGON, Chain.BASE, Chain.ARBITRUM_ONE, Chain.AVALANCHE, Chain.LINEA);
    }

    @Test
    void bscAndArbTickersAlsoReplicateAcrossEvmChains() {
        assertThat(mapping.chainsFor("BSC")).isEqualTo(mapping.chainsFor("ETH"));
        assertThat(mapping.chainsFor("ARB")).isEqualTo(mapping.chainsFor("ETH"));
    }

    @Test
    void nonEvmTickersMapToTheirSingleChain() {
        assertThat(mapping.chainsFor("XBT")).containsExactly(Chain.BITCOIN);
        assertThat(mapping.chainsFor("TRX")).containsExactly(Chain.TRON);
        assertThat(mapping.chainsFor("SOL")).containsExactly(Chain.SOLANA);
    }

    @Test
    void unsupportedTickersMapToNoChain() {
        assertThat(mapping.chainsFor("XMR")).isEmpty();
        assertThat(mapping.chainsFor("LTC")).isEmpty();
    }
}
