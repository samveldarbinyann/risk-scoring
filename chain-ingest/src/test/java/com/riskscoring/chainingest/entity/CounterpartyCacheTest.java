package com.riskscoring.chainingest.entity;

import com.riskscoring.common.model.TransferDirection;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CounterpartyCacheTest {

    @Test
    void instancesWithSameNonNullIdAreEqual() {
        UUID id = UUID.randomUUID();
        CounterpartyCache a = counterpartyCache(id);
        CounterpartyCache b = counterpartyCache(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void instancesWithNullIdAreNeverEqualToEachOther() {
        CounterpartyCache a = counterpartyCache(null);
        CounterpartyCache b = counterpartyCache(null);

        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void isNotEqualToNullOrUnrelatedType() {
        CounterpartyCache a = counterpartyCache(UUID.randomUUID());

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not a CounterpartyCache");
    }

    private static CounterpartyCache counterpartyCache(UUID id) {
        return CounterpartyCache.builder()
                .id(id)
                .address("addr")
                .direction(TransferDirection.IN)
                .txCount(1)
                .totalValueNative(BigInteger.ONE)
                .hops(1)
                .build();
    }
}
