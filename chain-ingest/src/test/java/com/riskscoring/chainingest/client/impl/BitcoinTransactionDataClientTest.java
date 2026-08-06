package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.MempoolApi;
import com.riskscoring.chainingest.client.dto.mempool.MempoolTransaction;
import com.riskscoring.chainingest.mapper.BitcoinTransactionSnapshotMapper;
import com.riskscoring.chainingest.mapper.BitcoinValues;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.TransactionFacts;
import com.riskscoring.common.model.TransactionSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BitcoinTransactionDataClientTest {

    @Mock
    private MempoolApi mempoolApi;

    @Mock
    private BitcoinTransactionSnapshotMapper bitcoinTransactionSnapshotMapper;

    private BitcoinTransactionDataClient client;

    @BeforeEach
    void setUp() {
        client = new BitcoinTransactionDataClient(mempoolApi, bitcoinTransactionSnapshotMapper, new BitcoinValues());
    }

    @Test
    void familyAndTargetAreBitcoinTransaction() {
        assertThat(client.family()).isEqualTo(ChainFamily.BITCOIN);
        assertThat(client.target()).isEqualTo(ScanTarget.TRANSACTION);
    }

    @Test
    void fetchNormalizesHashAndDelegatesToMapper() {
        MempoolTransaction transaction = new MempoolTransaction("tx-hash", 0, List.of(), List.of(), null);
        when(mempoolApi.transaction("tx-hash")).thenReturn(transaction);
        TransactionSnapshot snapshot = new TransactionSnapshot(
                "tx-hash", "from", "to", "100", true, Instant.now(), List.of(), 0, 0, List.of(), Instant.now());
        when(bitcoinTransactionSnapshotMapper.fromMempool(transaction)).thenReturn(snapshot);

        TransactionFacts facts = client.fetch("  tx-hash  ", Chain.BITCOIN);

        assertThat(facts.transaction()).isSameAs(snapshot);
    }
}
