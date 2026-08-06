package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.MoralisApi;
import com.riskscoring.chainingest.client.dto.MoralisActiveChain;
import com.riskscoring.chainingest.client.dto.MoralisHistoryEnvelope;
import com.riskscoring.chainingest.client.dto.MoralisTokenBalance;
import com.riskscoring.chainingest.client.dto.MoralisTxRef;
import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.mapper.CounterpartyAggregator;
import com.riskscoring.chainingest.mapper.MoralisValues;
import com.riskscoring.chainingest.mapper.TransferMapper;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.TokenBalance;
import com.riskscoring.common.model.TransferDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoralisAddressDataClientTest {

    private static final Chain CHAIN = Chain.ETHEREUM;

    @Mock
    private MoralisApi moralisApi;

    @Mock
    private TransferMapper transferMapper;

    @Mock
    private CounterpartyAggregator counterpartyAggregator;

    @Mock
    private ChainIngestProperties properties;

    private MoralisAddressDataClient client;

    @BeforeEach
    void setUp() {
        client = new MoralisAddressDataClient(moralisApi, new MoralisValues(), transferMapper,
                counterpartyAggregator, properties);
        lenient().when(properties.moralis()).thenReturn(provider(3));
        lenient().when(properties.maxTokenBalances()).thenReturn(10);
        lenient().when(moralisApi.balanceNative("0xabc", CHAIN)).thenReturn("1000");
        lenient().when(moralisApi.tokenBalances("0xabc", CHAIN)).thenReturn(List.of());
        lenient().when(moralisApi.walletActivity("0xabc", CHAIN)).thenReturn(Optional.empty());
    }

    @Test
    void familyAndTargetAreEvmAddress() {
        assertThat(client.family()).isEqualTo(ChainFamily.EVM);
        assertThat(client.target()).isEqualTo(ScanTarget.ADDRESS);
    }

    @Test
    void fetchBuildsTransferSampleAndCallsGraphWithConfiguredMaxHops() {
        MoralisHistoryEnvelope envelope = new MoralisHistoryEnvelope(null, List.of());
        when(moralisApi.walletHistory("0xabc", CHAIN)).thenReturn(envelope);
        List<Transfer> transfers = List.of(new Transfer("0xcp", TransferDirection.OUT, BigInteger.TEN, Instant.now()));
        when(transferMapper.fromTransactions(envelope.result(), "0xabc")).thenReturn(transfers);
        when(counterpartyAggregator.graph(eq("0xabc"), eq(transfers), eq(3), any())).thenReturn(List.of());

        client.fetch("0xABC", CHAIN);

        verify(counterpartyAggregator).graph(eq("0xabc"), eq(transfers), eq(3), any());
    }

    @Test
    void expandFunctionRefetchesWalletHistoryForCounterpartyAddress() {
        MoralisHistoryEnvelope envelope = new MoralisHistoryEnvelope(null, List.of());
        when(moralisApi.walletHistory("0xabc", CHAIN)).thenReturn(envelope);
        when(transferMapper.fromTransactions(any(), eq("0xabc"))).thenReturn(List.of());

        ArgumentCaptor<Function<String, List<Transfer>>> expandCaptor = ArgumentCaptor.forClass(Function.class);
        when(counterpartyAggregator.graph(eq("0xabc"), any(), anyInt(), expandCaptor.capture())).thenReturn(List.of());

        client.fetch("0xABC", CHAIN);

        MoralisHistoryEnvelope counterpartyEnvelope = new MoralisHistoryEnvelope(null, List.of());
        when(moralisApi.walletHistory("0xcp", CHAIN)).thenReturn(counterpartyEnvelope);
        List<Transfer> counterpartyTransfers = List.of(new Transfer("y", TransferDirection.IN, BigInteger.ONE, Instant.now()));
        when(transferMapper.fromTransactions(counterpartyEnvelope.result(), "0xcp")).thenReturn(counterpartyTransfers);

        List<Transfer> expanded = expandCaptor.getValue().apply("0xcp");

        assertThat(expanded).isEqualTo(counterpartyTransfers);
        verify(moralisApi).walletHistory("0xcp", CHAIN);
    }

    @Test
    void firstSeenAtPrefersActivityFirstTransactionTimestamp() {
        stubEmptyHistory();
        MoralisActiveChain activity = new MoralisActiveChain("eth", "0x1",
                new MoralisTxRef(null, "2024-01-01T00:00:00Z", null), null);
        when(moralisApi.walletActivity("0xabc", CHAIN)).thenReturn(Optional.of(activity));
        when(transferMapper.timestamp("2024-01-01T00:00:00Z")).thenReturn(Instant.parse("2024-01-01T00:00:00Z"));

        AddressFacts facts = client.fetch("0xABC", CHAIN);

        assertThat(facts.snapshot().firstSeenAt()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
    }

    @Test
    void lastSeenAtFallsBackToSampleLastActivityAtWhenNoWalletActivity() {
        Instant transferAt = Instant.parse("2024-02-01T00:00:00Z");
        MoralisHistoryEnvelope envelope = new MoralisHistoryEnvelope(null, List.of());
        when(moralisApi.walletHistory("0xabc", CHAIN)).thenReturn(envelope);
        List<Transfer> transfers = List.of(new Transfer("0xcp", TransferDirection.OUT, BigInteger.TEN, transferAt));
        when(transferMapper.fromTransactions(envelope.result(), "0xabc")).thenReturn(transfers);
        when(counterpartyAggregator.graph(eq("0xabc"), eq(transfers), anyInt(), any())).thenReturn(List.of());
        when(moralisApi.walletActivity("0xabc", CHAIN)).thenReturn(Optional.empty());

        AddressFacts facts = client.fetch("0xABC", CHAIN);

        assertThat(facts.snapshot().lastSeenAt()).isEqualTo(transferAt);
    }

    @Test
    void tokenBalancesSortedByUsdValueDescendingWithNullTreatedAsZeroAndTruncated() {
        stubEmptyHistory();
        lenient().when(properties.maxTokenBalances()).thenReturn(2);
        when(moralisApi.tokenBalances("0xabc", CHAIN)).thenReturn(List.of(
                new MoralisTokenBalance("A", "1", null),
                new MoralisTokenBalance("B", "2", 50.0),
                new MoralisTokenBalance("C", "3", 100.0)));

        AddressFacts facts = client.fetch("0xABC", CHAIN);

        assertThat(facts.snapshot().tokenBalances()).containsExactly(
                new TokenBalance("C", "3", 100.0),
                new TokenBalance("B", "2", 50.0));
    }

    private void stubEmptyHistory() {
        MoralisHistoryEnvelope envelope = new MoralisHistoryEnvelope(null, List.of());
        when(moralisApi.walletHistory("0xabc", CHAIN)).thenReturn(envelope);
        when(transferMapper.fromTransactions(any(), eq("0xabc"))).thenReturn(List.of());
        lenient().when(counterpartyAggregator.graph(eq("0xabc"), any(), anyInt(), any())).thenReturn(List.of());
    }

    private static ChainIngestProperties.Provider provider(int maxHops) {
        return new ChainIngestProperties.Provider(null, null, 0, maxHops, 0, 0, null, null, null);
    }
}
