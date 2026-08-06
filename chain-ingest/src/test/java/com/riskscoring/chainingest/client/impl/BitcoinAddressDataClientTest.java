package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.MempoolApi;
import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.client.dto.mempool.MempoolAddressStats;
import com.riskscoring.chainingest.client.dto.mempool.MempoolChainStats;
import com.riskscoring.chainingest.client.dto.mempool.MempoolStatus;
import com.riskscoring.chainingest.client.dto.mempool.MempoolTransaction;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.mapper.BitcoinTransferMapper;
import com.riskscoring.chainingest.mapper.BitcoinValues;
import com.riskscoring.chainingest.mapper.CounterpartyAggregator;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.ScanTarget;
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
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BitcoinAddressDataClientTest {

    @Mock
    private MempoolApi mempoolApi;

    @Mock
    private BitcoinTransferMapper bitcoinTransferMapper;

    @Mock
    private CounterpartyAggregator counterpartyAggregator;

    @Mock
    private ChainIngestProperties properties;

    private BitcoinAddressDataClient client;

    @BeforeEach
    void setUp() {
        client = new BitcoinAddressDataClient(mempoolApi, bitcoinTransferMapper, new BitcoinValues(),
                counterpartyAggregator, properties);
        lenient().when(properties.mempool()).thenReturn(provider(3));
    }

    @Test
    void familyAndTargetAreBitcoinAddress() {
        assertThat(client.family()).isEqualTo(ChainFamily.BITCOIN);
        assertThat(client.target()).isEqualTo(ScanTarget.ADDRESS);
    }

    @Test
    void fetchBuildsTransfersAndCallsGraphWithConfiguredMaxHops() {
        MempoolTransaction transaction = new MempoolTransaction("tx1", 0, List.of(), List.of(), null);
        when(mempoolApi.addressTransactions("addr")).thenReturn(List.of(transaction));
        List<Transfer> transfers = List.of(new Transfer("cp", TransferDirection.OUT, BigInteger.TEN, Instant.now()));
        when(bitcoinTransferMapper.fromTransactions(List.of(transaction), "addr")).thenReturn(transfers);
        when(mempoolApi.addressStats("addr")).thenReturn(addressStats(10, 1000, 500));
        when(counterpartyAggregator.graph(eq("addr"), eq(transfers), eq(3), any())).thenReturn(List.of());

        client.fetch("addr", Chain.BITCOIN);

        verify(counterpartyAggregator).graph(eq("addr"), eq(transfers), eq(3), any());
    }

    @Test
    void expandFunctionRefetchesTransfersForCounterpartyAddress() {
        MempoolTransaction transaction = new MempoolTransaction("tx1", 0, List.of(), List.of(), null);
        when(mempoolApi.addressTransactions("addr")).thenReturn(List.of(transaction));
        when(bitcoinTransferMapper.fromTransactions(any(), eq("addr"))).thenReturn(List.of());
        when(mempoolApi.addressStats("addr")).thenReturn(addressStats(10, 1000, 500));

        ArgumentCaptor<Function<String, List<Transfer>>> expandCaptor = ArgumentCaptor.forClass(Function.class);
        when(counterpartyAggregator.graph(eq("addr"), any(), anyInt(), expandCaptor.capture())).thenReturn(List.of());

        client.fetch("addr", Chain.BITCOIN);

        MempoolTransaction counterpartyTx = new MempoolTransaction("tx2", 0, List.of(), List.of(), null);
        when(mempoolApi.addressTransactions("counterparty-addr")).thenReturn(List.of(counterpartyTx));
        List<Transfer> counterpartyTransfers = List.of(new Transfer("x", TransferDirection.IN, BigInteger.ONE, Instant.now()));
        when(bitcoinTransferMapper.fromTransactions(List.of(counterpartyTx), "counterparty-addr")).thenReturn(counterpartyTransfers);

        List<Transfer> expanded = expandCaptor.getValue().apply("counterparty-addr");

        assertThat(expanded).isEqualTo(counterpartyTransfers);
        verify(mempoolApi).addressTransactions("counterparty-addr");
    }

    @Test
    void snapshotDerivesTxCountBalanceAndSampleTruncatedFromStats() {
        MempoolTransaction transaction = new MempoolTransaction("tx1", 0, List.of(), List.of(), null);
        when(mempoolApi.addressTransactions("addr")).thenReturn(List.of(transaction));
        when(bitcoinTransferMapper.fromTransactions(any(), eq("addr"))).thenReturn(List.of());
        when(mempoolApi.addressStats("addr")).thenReturn(addressStats(10, 1000, 500));
        when(counterpartyAggregator.graph(eq("addr"), any(), anyInt(), any())).thenReturn(List.of());

        AddressFacts facts = client.fetch("addr", Chain.BITCOIN);

        assertThat(facts.snapshot().txCount()).isEqualTo(10);
        assertThat(facts.snapshot().balanceNative()).isEqualTo("500");
        assertThat(facts.snapshot().sampleTruncated()).isTrue();
    }

    @Test
    void snapshotCountsOnlyTransactionsWithinTheTwentyFourHourWindow() {
        Instant now = Instant.now();
        MempoolStatus recentStatus = new MempoolStatus(true, 1L, now.minusSeconds(3600).getEpochSecond());
        MempoolStatus oldStatus = new MempoolStatus(true, 1L, now.minusSeconds(48 * 3600).getEpochSecond());
        MempoolTransaction recent = new MempoolTransaction("tx1", 0, List.of(), List.of(), recentStatus);
        MempoolTransaction old = new MempoolTransaction("tx2", 0, List.of(), List.of(), oldStatus);
        when(mempoolApi.addressTransactions("addr")).thenReturn(List.of(recent, old));
        when(bitcoinTransferMapper.fromTransactions(any(), eq("addr"))).thenReturn(List.of());
        when(mempoolApi.addressStats("addr")).thenReturn(addressStats(2, 1000, 500));
        when(counterpartyAggregator.graph(eq("addr"), any(), anyInt(), any())).thenReturn(List.of());

        AddressFacts facts = client.fetch("addr", Chain.BITCOIN);

        assertThat(facts.snapshot().txCount24h()).isEqualTo(1);
    }

    private static MempoolAddressStats addressStats(long txCount, long funded, long spent) {
        MempoolChainStats stats = new MempoolChainStats(0, funded, 0, spent, txCount);
        return new MempoolAddressStats("addr", stats, stats);
    }

    private static ChainIngestProperties.Provider provider(int maxHops) {
        return new ChainIngestProperties.Provider(null, null, 0, maxHops, 0, 0, null, null, null);
    }
}
