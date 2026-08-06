package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.TronGridApi;
import com.riskscoring.chainingest.client.dto.trongrid.TronContract;
import com.riskscoring.chainingest.client.dto.trongrid.TronContractValue;
import com.riskscoring.chainingest.client.dto.trongrid.TronParameter;
import com.riskscoring.chainingest.client.dto.trongrid.TronRawData;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransaction;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransactionInfo;
import com.riskscoring.chainingest.client.dto.trongrid.TronTrc20Transfer;
import com.riskscoring.chainingest.mapper.TronAddressCodec;
import com.riskscoring.chainingest.mapper.TronTransactionSnapshotMapper;
import com.riskscoring.chainingest.mapper.TronValues;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.TransactionFacts;
import com.riskscoring.common.model.TransactionSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TronTransactionDataClientTest {

    private static final String TXID = "txid";

    @Mock
    private TronGridApi tronGridApi;

    @Mock
    private TronTransactionSnapshotMapper tronTransactionSnapshotMapper;

    private TronTransactionDataClient client;

    @BeforeEach
    void setUp() {
        client = new TronTransactionDataClient(tronGridApi, tronTransactionSnapshotMapper,
                new TronValues(new TronAddressCodec()));
        lenient().when(tronTransactionSnapshotMapper.fromTronGrid(any(), any(), any())).thenReturn(snapshot());
    }

    @Test
    void familyAndTargetAreTronTransaction() {
        assertThat(client.family()).isEqualTo(ChainFamily.TRON);
        assertThat(client.target()).isEqualTo(ScanTarget.TRANSACTION);
    }

    @Test
    void tokenTransfersAreEmptyWhenBlockTimeIsZeroAndTrc20LookupIsNeverCalled() {
        TronTransaction transaction = new TronTransaction(TXID, 0L, null, new TronRawData(List.of()));
        when(tronGridApi.transaction(TXID)).thenReturn(transaction);
        when(tronGridApi.transactionInfo(TXID)).thenReturn(new TronTransactionInfo(null));

        client.fetch(TXID, Chain.TRON);

        verify(tronGridApi, never()).trc20TransfersAt(any(), org.mockito.ArgumentMatchers.anyLong());
        ArgumentCaptor<List<TronTrc20Transfer>> captor = ArgumentCaptor.forClass(List.class);
        verify(tronTransactionSnapshotMapper).fromTronGrid(eq(transaction), any(), captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void tokenTransfersFetchedOnlyForTriggerSmartContractWithRoutableOwnerAndFilteredByTxId() {
        TronContractValue value = new TronContractValue("Towner", "Tcontract", null);
        TronContract contract = new TronContract("TriggerSmartContract", new TronParameter(value));
        TronTransaction transaction = new TronTransaction(TXID, 0L, null, new TronRawData(List.of(contract)));
        when(tronGridApi.transaction(TXID)).thenReturn(transaction);
        when(tronGridApi.transactionInfo(TXID)).thenReturn(new TronTransactionInfo(500L));
        TronTrc20Transfer matching = new TronTrc20Transfer(TXID, 500L, "Towner", "Trecipient", "1", null);
        TronTrc20Transfer other = new TronTrc20Transfer("other-tx", 500L, "Towner", "Trecipient", "1", null);
        when(tronGridApi.trc20TransfersAt("Towner", 500L)).thenReturn(List.of(matching, other));

        client.fetch(TXID, Chain.TRON);

        ArgumentCaptor<List<TronTrc20Transfer>> captor = ArgumentCaptor.forClass(List.class);
        verify(tronTransactionSnapshotMapper).fromTronGrid(eq(transaction), any(), captor.capture());
        assertThat(captor.getValue()).containsExactly(matching);
    }

    @Test
    void fetchDelegatesToSnapshotMapper() {
        TronTransaction transaction = new TronTransaction(TXID, 0L, null, new TronRawData(List.of()));
        when(tronGridApi.transaction(TXID)).thenReturn(transaction);
        when(tronGridApi.transactionInfo(TXID)).thenReturn(new TronTransactionInfo(null));
        TransactionSnapshot snapshot = snapshot();
        when(tronTransactionSnapshotMapper.fromTronGrid(eq(transaction), any(), any())).thenReturn(snapshot);

        TransactionFacts facts = client.fetch(TXID, Chain.TRON);

        assertThat(facts.transaction()).isSameAs(snapshot);
    }

    private static TransactionSnapshot snapshot() {
        return new TransactionSnapshot(TXID, "from", "to", "0", true, Instant.now(), List.of(), 0, 0, List.of(), Instant.now());
    }
}
