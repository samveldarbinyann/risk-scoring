package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.HeliusApi;
import com.riskscoring.chainingest.client.dto.helius.HeliusTransaction;
import com.riskscoring.chainingest.mapper.SolanaTransactionSnapshotMapper;
import com.riskscoring.chainingest.mapper.SolanaValues;
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
class SolanaTransactionDataClientTest {

    @Mock
    private HeliusApi heliusApi;

    @Mock
    private SolanaTransactionSnapshotMapper solanaTransactionSnapshotMapper;

    private SolanaTransactionDataClient client;

    @BeforeEach
    void setUp() {
        client = new SolanaTransactionDataClient(heliusApi, solanaTransactionSnapshotMapper, new SolanaValues());
    }

    @Test
    void familyAndTargetAreSolanaTransaction() {
        assertThat(client.family()).isEqualTo(ChainFamily.SOLANA);
        assertThat(client.target()).isEqualTo(ScanTarget.TRANSACTION);
    }

    @Test
    void fetchNormalizesSignatureAndDelegatesToMapper() {
        HeliusTransaction transaction = new HeliusTransaction("sig", 1L, "fee", null, null, null);
        when(heliusApi.transaction("sig")).thenReturn(transaction);
        TransactionSnapshot snapshot = new TransactionSnapshot(
                "sig", "from", "to", "100", true, Instant.now(), List.of(), 0, 0, List.of(), Instant.now());
        when(solanaTransactionSnapshotMapper.fromHelius(transaction)).thenReturn(snapshot);

        TransactionFacts facts = client.fetch("  sig  ", Chain.SOLANA);

        assertThat(facts.transaction()).isSameAs(snapshot);
    }
}
