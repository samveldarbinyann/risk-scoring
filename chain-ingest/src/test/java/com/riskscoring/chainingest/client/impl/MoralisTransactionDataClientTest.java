package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.MoralisApi;
import com.riskscoring.chainingest.client.dto.MoralisTransaction;
import com.riskscoring.chainingest.mapper.MoralisValues;
import com.riskscoring.chainingest.mapper.TransactionSnapshotMapper;
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
class MoralisTransactionDataClientTest {

    @Mock
    private MoralisApi moralisApi;

    @Mock
    private TransactionSnapshotMapper transactionSnapshotMapper;

    private MoralisTransactionDataClient client;

    @BeforeEach
    void setUp() {
        client = new MoralisTransactionDataClient(moralisApi, new MoralisValues(), transactionSnapshotMapper);
    }

    @Test
    void familyAndTargetAreEvmTransaction() {
        assertThat(client.family()).isEqualTo(ChainFamily.EVM);
        assertThat(client.target()).isEqualTo(ScanTarget.TRANSACTION);
    }

    @Test
    void fetchNormalizesHashAndDelegatesToMapper() {
        MoralisTransaction transaction = new MoralisTransaction("0xhash", null, null, null, null, null, null, null);
        when(moralisApi.transaction("0xhash", Chain.ETHEREUM)).thenReturn(transaction);
        TransactionSnapshot snapshot = new TransactionSnapshot(
                "0xhash", "0xfrom", "0xto", "100", true, Instant.now(), List.of(), 0, 0, List.of(), Instant.now());
        when(transactionSnapshotMapper.fromMoralis(transaction)).thenReturn(snapshot);

        TransactionFacts facts = client.fetch("0xHASH", Chain.ETHEREUM);

        assertThat(facts.transaction()).isSameAs(snapshot);
    }
}
