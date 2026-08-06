package com.riskscoring.chainingest.entity;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.TransferDirection;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AddressCacheTest {

    @Test
    void replaceCounterpartiesClearsOldListAndSetsBackReference() {
        AddressCache cache = addressCache(UUID.randomUUID());
        CounterpartyCache stale = counterparty("stale-addr");
        cache.replaceCounterparties(List.of(stale));

        CounterpartyCache replacement1 = counterparty("addr-1");
        CounterpartyCache replacement2 = counterparty("addr-2");
        cache.replaceCounterparties(List.of(replacement1, replacement2));

        assertThat(cache.getCounterparties()).containsExactly(replacement1, replacement2);
        assertThat(replacement1.getAddressCache()).isSameAs(cache);
        assertThat(replacement2.getAddressCache()).isSameAs(cache);
    }

    @Test
    void replaceCounterpartiesWithEmptyListClearsCache() {
        AddressCache cache = addressCache(UUID.randomUUID());
        cache.replaceCounterparties(List.of(counterparty("addr-1")));

        cache.replaceCounterparties(List.of());

        assertThat(cache.getCounterparties()).isEmpty();
    }

    @Test
    void instancesWithSameNonNullIdAreEqual() {
        UUID id = UUID.randomUUID();
        AddressCache a = addressCache(id);
        AddressCache b = addressCache(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void instancesWithNullIdAreNeverEqualToEachOther() {
        AddressCache a = addressCache(null);
        AddressCache b = addressCache(null);

        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void isEqualToItself() {
        AddressCache a = addressCache(UUID.randomUUID());

        assertThat(a).isEqualTo(a);
    }

    @Test
    void isNotEqualToNullOrUnrelatedType() {
        AddressCache a = addressCache(UUID.randomUUID());

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not an AddressCache");
    }

    private static AddressCache addressCache(UUID id) {
        return AddressCache.builder()
                .id(id)
                .chain(Chain.ETHEREUM)
                .address("0xabc")
                .build();
    }

    private static CounterpartyCache counterparty(String address) {
        return CounterpartyCache.builder()
                .address(address)
                .direction(TransferDirection.OUT)
                .txCount(1)
                .totalValueNative(BigInteger.ONE)
                .hops(1)
                .build();
    }
}
