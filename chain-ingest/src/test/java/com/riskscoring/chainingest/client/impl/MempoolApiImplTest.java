package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.HttpCallTemplate;
import com.riskscoring.chainingest.client.dto.mempool.MempoolAddressStats;
import com.riskscoring.chainingest.client.dto.mempool.MempoolTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MempoolApiImplTest {

    @Mock
    private HttpCallTemplate mempoolCallTemplate;

    private MempoolApiImpl api;

    @Test
    void addressStatsCallsExpectedPath() {
        api = new MempoolApiImpl(mempoolCallTemplate);
        MempoolAddressStats stats = new MempoolAddressStats("addr", null, null);
        when(mempoolCallTemplate.get("/address/addr", MempoolAddressStats.class)).thenReturn(stats);

        assertThat(api.addressStats("addr")).isSameAs(stats);
    }

    @Test
    void addressTransactionsWrapsArrayResultInList() {
        api = new MempoolApiImpl(mempoolCallTemplate);
        MempoolTransaction tx = new MempoolTransaction("tx1", 0, null, null, null);
        when(mempoolCallTemplate.get("/address/addr/txs", MempoolTransaction[].class))
                .thenReturn(new MempoolTransaction[]{tx});

        assertThat(api.addressTransactions("addr")).containsExactly(tx);
    }

    @Test
    void transactionCallsExpectedPath() {
        api = new MempoolApiImpl(mempoolCallTemplate);
        MempoolTransaction tx = new MempoolTransaction("tx1", 0, null, null, null);
        when(mempoolCallTemplate.get("/tx/tx1", MempoolTransaction.class)).thenReturn(tx);

        assertThat(api.transaction("tx1")).isSameAs(tx);
    }
}
