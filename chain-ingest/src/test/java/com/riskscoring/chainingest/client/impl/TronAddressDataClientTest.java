package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.TronGridApi;
import com.riskscoring.chainingest.client.dto.trongrid.TronAccount;
import com.riskscoring.chainingest.client.dto.trongrid.TronTokenInfo;
import com.riskscoring.chainingest.client.dto.trongrid.TronTrc20Transfer;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.mapper.CounterpartyAggregator;
import com.riskscoring.chainingest.mapper.TronAddressCodec;
import com.riskscoring.chainingest.mapper.TronTransferMapper;
import com.riskscoring.chainingest.mapper.TronValues;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TronAddressDataClientTest {

    private static final String TARGET = "Ttarget";

    @Mock
    private TronGridApi tronGridApi;

    @Mock
    private TronTransferMapper tronTransferMapper;

    @Mock
    private CounterpartyAggregator counterpartyAggregator;

    @Mock
    private ChainIngestProperties properties;

    private TronAddressDataClient client;

    @BeforeEach
    void setUp() {
        client = new TronAddressDataClient(tronGridApi, new TronValues(new TronAddressCodec()),
                tronTransferMapper, counterpartyAggregator, properties);
        lenient().when(properties.tronGrid()).thenReturn(provider(3, 5));
        lenient().when(properties.maxTokenBalances()).thenReturn(10);
        lenient().when(tronTransferMapper.fromNative(any(), any())).thenReturn(List.of());
        lenient().when(tronTransferMapper.fromTrc20(any(), any())).thenReturn(List.of());
        lenient().when(counterpartyAggregator.graph(any(), any(), anyInt(), any())).thenReturn(List.of());
        lenient().when(tronGridApi.accountTransactions(TARGET)).thenReturn(List.of());
        lenient().when(tronGridApi.accountTrc20Transfers(TARGET)).thenReturn(List.of());
        lenient().when(tronGridApi.account(TARGET)).thenReturn(Optional.empty());
    }

    @Test
    void familyAndTargetAreTronAddress() {
        assertThat(client.family()).isEqualTo(ChainFamily.TRON);
        assertThat(client.target()).isEqualTo(ScanTarget.ADDRESS);
    }

    @Test
    void truncatedIsTrueWhenTokenTransferCountEqualsPageSize() {
        List<TronTrc20Transfer> transfers = List.of(trc20(null), trc20(null), trc20(null), trc20(null), trc20(null));
        when(tronGridApi.accountTrc20Transfers(TARGET)).thenReturn(transfers);

        AddressFacts facts = client.fetch(TARGET, Chain.TRON);

        assertThat(facts.snapshot().sampleTruncated()).isTrue();
    }

    @Test
    void truncatedIsFalseWhenTokenTransferCountIsBelowPageSize() {
        when(tronGridApi.accountTrc20Transfers(TARGET)).thenReturn(List.of(trc20(null)));

        AddressFacts facts = client.fetch(TARGET, Chain.TRON);

        assertThat(facts.snapshot().sampleTruncated()).isFalse();
    }

    @Test
    void balanceIsZeroWhenAccountIsAbsent() {
        AddressFacts facts = client.fetch(TARGET, Chain.TRON);

        assertThat(facts.snapshot().balanceNative()).isEqualTo("0");
    }

    @Test
    void balanceAndFirstSeenAtReflectAccountWhenPresent() {
        when(tronGridApi.account(TARGET)).thenReturn(Optional.of(new TronAccount(5000L, 1700000000L, List.of())));

        AddressFacts facts = client.fetch(TARGET, Chain.TRON);

        assertThat(facts.snapshot().balanceNative()).isEqualTo("5000");
        assertThat(facts.snapshot().firstSeenAt()).isNotNull();
    }

    @Test
    void tokenBalancesJoinHoldingsAgainstKnownTokensDedupingByAddressAndFilteringUnknown() {
        List<TronTrc20Transfer> tokenTransfers = List.of(
                trc20(new TronTokenInfo("TK1", "Ttoken1", 6)),
                trc20(new TronTokenInfo("TK1-DUPE", "Ttoken1", 6)),
                trc20(new TronTokenInfo("TK2", "Ttoken2", 6)));
        when(tronGridApi.accountTrc20Transfers(TARGET)).thenReturn(tokenTransfers);
        TronAccount account = new TronAccount(0L, 0L, List.of(
                Map.of("Ttoken1", "500000"),
                Map.of("Ttoken2", "2000000"),
                Map.of("Tunknown", "999")));
        when(tronGridApi.account(TARGET)).thenReturn(Optional.of(account));

        AddressFacts facts = client.fetch(TARGET, Chain.TRON);

        assertThat(facts.snapshot().tokenBalances()).containsExactly(
                new TokenBalance("TK2", "2.000000", null),
                new TokenBalance("TK1", "0.500000", null));
    }

    @Test
    void tokenBalancesAreTruncatedByMaxTokenBalances() {
        lenient().when(properties.maxTokenBalances()).thenReturn(1);
        List<TronTrc20Transfer> tokenTransfers = List.of(
                trc20(new TronTokenInfo("TK1", "Ttoken1", 6)),
                trc20(new TronTokenInfo("TK2", "Ttoken2", 6)));
        when(tronGridApi.accountTrc20Transfers(TARGET)).thenReturn(tokenTransfers);
        TronAccount account = new TronAccount(0L, 0L, List.of(
                Map.of("Ttoken1", "500000"),
                Map.of("Ttoken2", "2000000")));
        when(tronGridApi.account(TARGET)).thenReturn(Optional.of(account));

        AddressFacts facts = client.fetch(TARGET, Chain.TRON);

        assertThat(facts.snapshot().tokenBalances()).hasSize(1);
        assertThat(facts.snapshot().tokenBalances().getFirst().symbol()).isEqualTo("TK2");
    }

    private static TronTrc20Transfer trc20(TronTokenInfo tokenInfo) {
        return new TronTrc20Transfer("tx", 0L, TARGET, "Tother", "1", tokenInfo);
    }

    private static ChainIngestProperties.Provider provider(int maxHops, int pageSize) {
        return new ChainIngestProperties.Provider(null, null, pageSize, maxHops, 0, 0, null, null, null);
    }
}
