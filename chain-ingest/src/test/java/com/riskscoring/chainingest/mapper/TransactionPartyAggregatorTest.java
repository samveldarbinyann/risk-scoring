package com.riskscoring.chainingest.mapper;

import com.riskscoring.common.model.TransactionParty;
import com.riskscoring.common.model.TransactionRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TransactionPartyAggregatorTest {

    @Mock
    private ChainAddressValues values;

    private final TransactionPartyAggregator aggregator = new TransactionPartyAggregator();

    @Test
    void partyIsEmptyWhenAddressIsNotRoutable() {
        lenient().when(values.address("raw")).thenReturn("normalized");
        lenient().when(values.isRoutable("normalized")).thenReturn(false);

        assertThat(aggregator.party(values, "raw", TransactionRole.SENDER, BigInteger.TEN)).isEmpty();
    }

    @Test
    void partyIsPresentWhenAddressIsRoutable() {
        lenient().when(values.address("raw")).thenReturn("normalized");
        lenient().when(values.isRoutable("normalized")).thenReturn(true);

        assertThat(aggregator.party(values, "raw", TransactionRole.SENDER, BigInteger.TEN))
                .contains(new TransactionParty("normalized", TransactionRole.SENDER, "10"));
    }

    @Test
    void aggregateSumsValueForSameAddressAndRole() {
        List<TransactionParty> result = aggregator.aggregate(Stream.of(
                new TransactionParty("addr", TransactionRole.SENDER, "100"),
                new TransactionParty("addr", TransactionRole.SENDER, "200")));

        assertThat(result).containsExactly(new TransactionParty("addr", TransactionRole.SENDER, "300"));
    }

    @Test
    void aggregateKeepsDifferentRolesForSameAddressSeparate() {
        List<TransactionParty> result = aggregator.aggregate(Stream.of(
                new TransactionParty("addr", TransactionRole.SENDER, "100"),
                new TransactionParty("addr", TransactionRole.RECIPIENT, "50")));

        assertThat(result).containsExactlyInAnyOrder(
                new TransactionParty("addr", TransactionRole.SENDER, "100"),
                new TransactionParty("addr", TransactionRole.RECIPIENT, "50"));
    }

    @Test
    void aggregateSortsByRoleThenAddress() {
        List<TransactionParty> result = aggregator.aggregate(Stream.of(
                new TransactionParty("zzz", TransactionRole.SENDER, "1"),
                new TransactionParty("aaa", TransactionRole.SENDER, "1"),
                new TransactionParty("mmm", TransactionRole.RECIPIENT, "1")));

        assertThat(result).extracting(TransactionParty::address)
                .containsExactly("aaa", "zzz", "mmm");
    }

    @Test
    void aggregateOnEmptyStreamReturnsEmptyList() {
        assertThat(aggregator.aggregate(Stream.empty())).isEmpty();
    }
}
