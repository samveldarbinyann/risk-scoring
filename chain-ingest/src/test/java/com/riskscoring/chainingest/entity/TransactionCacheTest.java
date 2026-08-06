package com.riskscoring.chainingest.entity;

import com.riskscoring.common.model.Chain;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionCacheTest {

    @Test
    void instancesWithSameNonNullIdAreEqual() {
        UUID id = UUID.randomUUID();
        TransactionCache a = transactionCache(id);
        TransactionCache b = transactionCache(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void instancesWithNullIdAreNeverEqualToEachOther() {
        TransactionCache a = transactionCache(null);
        TransactionCache b = transactionCache(null);

        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void isEqualToItself() {
        TransactionCache a = transactionCache(UUID.randomUUID());

        assertThat(a).isEqualTo(a);
    }

    @Test
    void isNotEqualToNullOrUnrelatedType() {
        TransactionCache a = transactionCache(UUID.randomUUID());

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not a TransactionCache");
    }

    private static TransactionCache transactionCache(UUID id) {
        return TransactionCache.builder()
                .id(id)
                .chain(Chain.BITCOIN)
                .txHash("hash")
                .valueNative(BigInteger.ONE)
                .parties("[]")
                .tokenTransfers("[]")
                .build();
    }
}
