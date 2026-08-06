package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.HeliusApi;
import com.riskscoring.chainingest.client.dto.helius.HeliusAsset;
import com.riskscoring.chainingest.client.dto.helius.HeliusNativeBalance;
import com.riskscoring.chainingest.client.dto.helius.HeliusPortfolio;
import com.riskscoring.chainingest.client.dto.helius.HeliusPriceInfo;
import com.riskscoring.chainingest.client.dto.helius.HeliusTokenInfo;
import com.riskscoring.chainingest.client.dto.helius.HeliusTransaction;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.mapper.CounterpartyAggregator;
import com.riskscoring.chainingest.mapper.SolanaTransferMapper;
import com.riskscoring.chainingest.mapper.SolanaValues;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.TokenBalance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolanaAddressDataClientTest {

    @Mock
    private HeliusApi heliusApi;

    @Mock
    private SolanaTransferMapper solanaTransferMapper;

    @Mock
    private CounterpartyAggregator counterpartyAggregator;

    @Mock
    private ChainIngestProperties properties;

    private SolanaAddressDataClient client;

    @BeforeEach
    void setUp() {
        client = new SolanaAddressDataClient(heliusApi, new SolanaValues(), solanaTransferMapper,
                counterpartyAggregator, properties);
        lenient().when(properties.helius()).thenReturn(provider(3, 5));
        lenient().when(properties.maxTokenBalances()).thenReturn(10);
        lenient().when(solanaTransferMapper.fromTransactions(any(), any())).thenReturn(List.of());
        lenient().when(counterpartyAggregator.graph(any(), any(), anyInt(), any())).thenReturn(List.of());
        lenient().when(heliusApi.portfolio("addr")).thenReturn(new HeliusPortfolio(List.of(), null));
    }

    @Test
    void familyAndTargetAreSolanaAddress() {
        assertThat(client.family()).isEqualTo(ChainFamily.SOLANA);
        assertThat(client.target()).isEqualTo(ScanTarget.ADDRESS);
    }

    @Test
    void truncatedIsTrueWhenTransactionCountEqualsPageSize() {
        when(heliusApi.addressTransactions("addr")).thenReturn(List.of(
                tx("s1"), tx("s2"), tx("s3"), tx("s4"), tx("s5")));

        AddressFacts facts = client.fetch("addr", Chain.SOLANA);

        assertThat(facts.snapshot().sampleTruncated()).isTrue();
    }

    @Test
    void truncatedIsFalseWhenTransactionCountIsOneLessThanPageSize() {
        when(heliusApi.addressTransactions("addr")).thenReturn(List.of(
                tx("s1"), tx("s2"), tx("s3"), tx("s4")));

        AddressFacts facts = client.fetch("addr", Chain.SOLANA);

        assertThat(facts.snapshot().sampleTruncated()).isFalse();
    }

    @Test
    void balanceLamportsIsZeroWhenNativeBalanceMissing() {
        when(heliusApi.addressTransactions("addr")).thenReturn(List.of());
        when(heliusApi.portfolio("addr")).thenReturn(new HeliusPortfolio(List.of(), null));

        AddressFacts facts = client.fetch("addr", Chain.SOLANA);

        assertThat(facts.snapshot().balanceNative()).isEqualTo("0");
    }

    @Test
    void balanceLamportsReflectsNativeBalanceWhenPresent() {
        when(heliusApi.addressTransactions("addr")).thenReturn(List.of());
        when(heliusApi.portfolio("addr")).thenReturn(new HeliusPortfolio(List.of(), new HeliusNativeBalance(500L)));

        AddressFacts facts = client.fetch("addr", Chain.SOLANA);

        assertThat(facts.snapshot().balanceNative()).isEqualTo("500");
    }

    @Test
    void tokenBalancesFilterNullTokenInfoSortByUsdValueAndTruncate() {
        lenient().when(properties.maxTokenBalances()).thenReturn(2);
        when(heliusApi.addressTransactions("addr")).thenReturn(List.of());
        HeliusAsset withoutTokenInfo = new HeliusAsset(null);
        HeliusAsset low = new HeliusAsset(new HeliusTokenInfo("A", "1000", 3, new HeliusPriceInfo(10.0)));
        HeliusAsset high = new HeliusAsset(new HeliusTokenInfo("B", "2000", 3, new HeliusPriceInfo(100.0)));
        when(heliusApi.portfolio("addr")).thenReturn(new HeliusPortfolio(List.of(withoutTokenInfo, low, high), null));

        AddressFacts facts = client.fetch("addr", Chain.SOLANA);

        assertThat(facts.snapshot().tokenBalances()).containsExactly(
                new TokenBalance("B", "2.000", 100.0),
                new TokenBalance("A", "1.000", 10.0));
    }

    private static HeliusTransaction tx(String signature) {
        return new HeliusTransaction(signature, 1700000000L, "fee", null, null, null);
    }

    private static ChainIngestProperties.Provider provider(int maxHops, int pageSize) {
        return new ChainIngestProperties.Provider(null, null, pageSize, maxHops, 0, 0, null, null, null);
    }
}
