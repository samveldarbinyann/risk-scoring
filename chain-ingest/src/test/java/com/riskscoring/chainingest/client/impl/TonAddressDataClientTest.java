package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.TonApi;
import com.riskscoring.chainingest.client.dto.tonapi.TonAccount;
import com.riskscoring.chainingest.client.dto.tonapi.TonEvent;
import com.riskscoring.chainingest.client.dto.tonapi.TonJettonBalance;
import com.riskscoring.chainingest.client.dto.tonapi.TonJettonPreview;
import com.riskscoring.chainingest.client.dto.tonapi.TonPrice;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.mapper.CounterpartyAggregator;
import com.riskscoring.chainingest.mapper.TonTransferMapper;
import com.riskscoring.chainingest.mapper.TonValues;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TonAddressDataClientTest {

    @Mock
    private TonApi tonApi;

    @Mock
    private TonTransferMapper tonTransferMapper;

    @Mock
    private CounterpartyAggregator counterpartyAggregator;

    @Mock
    private ChainIngestProperties properties;

    private TonAddressDataClient client;

    @BeforeEach
    void setUp() {
        client = new TonAddressDataClient(tonApi, new TonValues(), tonTransferMapper, counterpartyAggregator, properties);
        lenient().when(properties.tonApi()).thenReturn(provider(3, 5));
        lenient().when(properties.maxTokenBalances()).thenReturn(10);
        lenient().when(tonTransferMapper.fromEvents(any(), any())).thenReturn(List.of());
        lenient().when(counterpartyAggregator.graph(any(), any(), anyInt(), any())).thenReturn(List.of());
        lenient().when(tonApi.account("addr")).thenReturn(new TonAccount(null));
        lenient().when(tonApi.jettons("addr")).thenReturn(List.of());
        lenient().when(tonApi.firstEvent("addr")).thenReturn(Optional.empty());
    }

    @Test
    void familyAndTargetAreTonAddress() {
        assertThat(client.family()).isEqualTo(ChainFamily.TON);
        assertThat(client.target()).isEqualTo(ScanTarget.ADDRESS);
    }

    @Test
    void truncatedIsTrueWhenEventCountEqualsPageSize() {
        when(tonApi.accountEvents("addr")).thenReturn(List.of(
                event("e1"), event("e2"), event("e3"), event("e4"), event("e5")));

        AddressFacts facts = client.fetch("addr", Chain.TON);

        assertThat(facts.snapshot().sampleTruncated()).isTrue();
    }

    @Test
    void truncatedIsFalseWhenEventCountIsBelowPageSize() {
        when(tonApi.accountEvents("addr")).thenReturn(List.of(event("e1")));

        AddressFacts facts = client.fetch("addr", Chain.TON);

        assertThat(facts.snapshot().sampleTruncated()).isFalse();
    }

    @Test
    void balanceIsZeroWhenAccountBalanceIsNull() {
        when(tonApi.accountEvents("addr")).thenReturn(List.of());

        AddressFacts facts = client.fetch("addr", Chain.TON);

        assertThat(facts.snapshot().balanceNative()).isEqualTo("0");
    }

    @Test
    void firstSeenAtIsNullWhenNoFirstEvent() {
        when(tonApi.accountEvents("addr")).thenReturn(List.of());

        AddressFacts facts = client.fetch("addr", Chain.TON);

        assertThat(facts.snapshot().firstSeenAt()).isNull();
    }

    @Test
    void firstSeenAtComesFromFirstEventTimestamp() {
        when(tonApi.accountEvents("addr")).thenReturn(List.of());
        when(tonApi.firstEvent("addr")).thenReturn(Optional.of(new TonEvent("evt", 1700000000L, false, List.of())));

        AddressFacts facts = client.fetch("addr", Chain.TON);

        assertThat(facts.snapshot().firstSeenAt()).isEqualTo(Instant.ofEpochSecond(1700000000L));
    }

    @Test
    void tokenBalancesFilterNullJettonSortByUsdValueAndTruncate() {
        lenient().when(properties.maxTokenBalances()).thenReturn(2);
        when(tonApi.accountEvents("addr")).thenReturn(List.of());
        TonJettonBalance noJetton = new TonJettonBalance("100", null, null);
        TonJettonBalance low = new TonJettonBalance("1000000", new TonJettonPreview("addr-a", "A", 6),
                new TonPrice(Map.of("USD", 1.0)));
        TonJettonBalance high = new TonJettonBalance("1000000", new TonJettonPreview("addr-b", "B", 6),
                new TonPrice(Map.of("USD", 100.0)));
        when(tonApi.jettons("addr")).thenReturn(List.of(noJetton, low, high));

        AddressFacts facts = client.fetch("addr", Chain.TON);

        assertThat(facts.snapshot().tokenBalances()).containsExactly(
                new TokenBalance("B", "1.000000", 100.0),
                new TokenBalance("A", "1.000000", 1.0));
    }

    private static TonEvent event(String id) {
        return new TonEvent(id, 1700000000L, false, List.of());
    }

    private static ChainIngestProperties.Provider provider(int maxHops, int pageSize) {
        return new ChainIngestProperties.Provider(null, null, pageSize, maxHops, 0, 0, null, null, null);
    }
}
