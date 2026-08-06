package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.TonApi;
import com.riskscoring.chainingest.client.dto.tonapi.TonEvent;
import com.riskscoring.chainingest.mapper.TonTransactionSnapshotMapper;
import com.riskscoring.chainingest.mapper.TonValues;
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
class TonTransactionDataClientTest {

    @Mock
    private TonApi tonApi;

    @Mock
    private TonTransactionSnapshotMapper tonTransactionSnapshotMapper;

    private TonTransactionDataClient client;

    @BeforeEach
    void setUp() {
        client = new TonTransactionDataClient(tonApi, tonTransactionSnapshotMapper, new TonValues());
    }

    @Test
    void familyAndTargetAreTonTransaction() {
        assertThat(client.family()).isEqualTo(ChainFamily.TON);
        assertThat(client.target()).isEqualTo(ScanTarget.TRANSACTION);
    }

    @Test
    void fetchNormalizesHashAndPassesItToBothEventLookupAndSnapshotMapper() {
        TonEvent event = new TonEvent("evt-hash", 1L, false, List.of());
        when(tonApi.event("evt-hash")).thenReturn(event);
        TransactionSnapshot snapshot = new TransactionSnapshot(
                "evt-hash", "from", "to", "100", true, Instant.now(), List.of(), 0, 0, List.of(), Instant.now());
        when(tonTransactionSnapshotMapper.fromTonApi(event, "evt-hash")).thenReturn(snapshot);

        TransactionFacts facts = client.fetch("  EVT-HASH  ", Chain.TON);

        assertThat(facts.transaction()).isSameAs(snapshot);
    }
}
