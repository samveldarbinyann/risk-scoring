package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.common.model.Counterparty;
import com.riskscoring.common.model.TransferDirection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterpartyAggregatorTest {

    private static final Instant AT = Instant.parse("2024-01-01T00:00:00Z");
    private static final String TARGET = "target";

    @Mock
    private Function<String, List<Transfer>> expand;

    @Test
    void emptyTransfersProduceEmptyGraphAndNeverExpand() {
        CounterpartyAggregator aggregator = new CounterpartyAggregator(properties(10, 5, 5));

        List<Counterparty> result = aggregator.graph(TARGET, List.of(), 2, expand);

        assertThat(result).isEmpty();
        verify(expand, never()).apply(any());
    }

    @Test
    void maxHopsOneNeverExpandsSecondHopEvenWithData() {
        CounterpartyAggregator aggregator = new CounterpartyAggregator(properties(10, 5, 5));
        List<Transfer> transfers = List.of(transfer("addr1", TransferDirection.OUT, 100));

        List<Counterparty> result = aggregator.graph(TARGET, transfers, 1, expand);

        assertThat(result).extracting(Counterparty::address).containsExactly("addr1");
        verify(expand, never()).apply(any());
    }

    @Test
    void directionIsOutWhenOnlyOutgoingTransfersExist() {
        CounterpartyAggregator aggregator = new CounterpartyAggregator(properties(10, 5, 5));
        List<Transfer> transfers = List.of(transfer("addr1", TransferDirection.OUT, 100));

        List<Counterparty> result = aggregator.graph(TARGET, transfers, 1, expand);

        assertThat(result).containsExactly(new Counterparty("addr1", TransferDirection.OUT, 1, "100", 1));
    }

    @Test
    void directionIsInWhenOnlyIncomingTransfersExist() {
        CounterpartyAggregator aggregator = new CounterpartyAggregator(properties(10, 5, 5));
        List<Transfer> transfers = List.of(transfer("addr1", TransferDirection.IN, 100));

        List<Counterparty> result = aggregator.graph(TARGET, transfers, 1, expand);

        assertThat(result).containsExactly(new Counterparty("addr1", TransferDirection.IN, 1, "100", 1));
    }

    @Test
    void directionIsBothWhenIncomingAndOutgoingTransfersExist() {
        CounterpartyAggregator aggregator = new CounterpartyAggregator(properties(10, 5, 5));
        List<Transfer> transfers = List.of(
                transfer("addr1", TransferDirection.OUT, 100),
                transfer("addr1", TransferDirection.IN, 50));

        List<Counterparty> result = aggregator.graph(TARGET, transfers, 1, expand);

        assertThat(result).containsExactly(new Counterparty("addr1", TransferDirection.BOTH, 2, "150", 1));
    }

    @Test
    void firstHopSortedByRelevanceTxCountThenValueDescending() {
        CounterpartyAggregator aggregator = new CounterpartyAggregator(properties(10, 5, 5));
        List<Transfer> transfers = List.of(
                transfer("addr-a", TransferDirection.OUT, 1000),
                transfer("addr-b", TransferDirection.OUT, 10),
                transfer("addr-b", TransferDirection.OUT, 10),
                transfer("addr-b", TransferDirection.OUT, 10),
                transfer("addr-c", TransferDirection.OUT, 100),
                transfer("addr-c", TransferDirection.OUT, 100),
                transfer("addr-c", TransferDirection.OUT, 100));

        List<Counterparty> result = aggregator.graph(TARGET, transfers, 1, expand);

        assertThat(result).extracting(Counterparty::address).containsExactly("addr-c", "addr-b", "addr-a");
    }

    @Test
    void secondHopExpandsOnlyTopHop2ExpandTopFirstHopCounterparties() {
        CounterpartyAggregator aggregator = new CounterpartyAggregator(properties(100, 2, 100));
        List<Transfer> transfers = List.of(
                transfer("n0", TransferDirection.OUT, 300),
                transfer("n1", TransferDirection.OUT, 200),
                transfer("n2", TransferDirection.OUT, 100));
        when(expand.apply(anyString())).thenReturn(List.of());

        aggregator.graph(TARGET, transfers, 2, expand);

        verify(expand, times(2)).apply(anyString());
    }

    @Test
    void secondHopDedupesAddressesAlreadyKnownOrEqualToTarget() {
        CounterpartyAggregator aggregator = new CounterpartyAggregator(properties(100, 1, 100));
        List<Transfer> transfers = List.of(transfer("known1", TransferDirection.OUT, 100));
        when(expand.apply("known1")).thenReturn(List.of(
                transfer("known1", TransferDirection.OUT, 1),
                transfer(TARGET, TransferDirection.OUT, 1),
                transfer("new-addr", TransferDirection.OUT, 50)));

        List<Counterparty> result = aggregator.graph(TARGET, transfers, 2, expand);

        assertThat(result).extracting(Counterparty::address).containsExactlyInAnyOrder("known1", "new-addr");
        Counterparty secondHopCounterparty = result.stream()
                .filter(counterparty -> counterparty.address().equals("new-addr"))
                .findFirst()
                .orElseThrow();
        assertThat(secondHopCounterparty.hops()).isEqualTo(2);
    }

    @Test
    void mergeCapsNearAtLimitMinusHop2ReserveWhenFarthestExceedsReserve() {
        // limit=5, hop2Reserve=2, farthest.size()=4 (>reserve) -> near = 5 - min(4,2) = 3, far = 5 - 3 = 2
        CounterpartyAggregator aggregator = new CounterpartyAggregator(properties(5, 1, 2));
        List<Transfer> nearestTransfers = descendingValueTransfers("n", 10);
        List<Transfer> farthestTransfers = descendingValueTransfers("f", 4);
        when(expand.apply(anyString())).thenReturn(farthestTransfers);

        List<Counterparty> result = aggregator.graph(TARGET, nearestTransfers, 2, expand);

        assertThat(result).extracting(Counterparty::address)
                .containsExactlyInAnyOrder("n0", "n1", "n2", "f0", "f1");
    }

    @Test
    void mergeReservesOnlyWhatFarthestActuallyNeedsWhenWithinReserve() {
        // limit=5, hop2Reserve=5, farthest.size()=4 (<=reserve) -> near = 5 - min(4,5) = 1, far = 5 - 1 = 4 (all)
        CounterpartyAggregator aggregator = new CounterpartyAggregator(properties(5, 1, 5));
        List<Transfer> nearestTransfers = descendingValueTransfers("n", 10);
        List<Transfer> farthestTransfers = descendingValueTransfers("f", 4);
        when(expand.apply(anyString())).thenReturn(farthestTransfers);

        List<Counterparty> result = aggregator.graph(TARGET, nearestTransfers, 2, expand);

        assertThat(result).extracting(Counterparty::address)
                .containsExactlyInAnyOrder("n0", "f0", "f1", "f2", "f3");
    }

    @Test
    void mergeIncludesEverythingWhenBothHopsFitUnderLimit() {
        CounterpartyAggregator aggregator = new CounterpartyAggregator(properties(100, 1, 100));
        List<Transfer> nearestTransfers = descendingValueTransfers("n", 10);
        List<Transfer> farthestTransfers = descendingValueTransfers("f", 4);
        when(expand.apply(anyString())).thenReturn(farthestTransfers);

        List<Counterparty> result = aggregator.graph(TARGET, nearestTransfers, 2, expand);

        assertThat(result).hasSize(14);
    }

    @Test
    void resultSortedByProximityHopsAscendingThenRelevance() {
        CounterpartyAggregator aggregator = new CounterpartyAggregator(properties(5, 1, 2));
        List<Transfer> nearestTransfers = descendingValueTransfers("n", 10);
        List<Transfer> farthestTransfers = descendingValueTransfers("f", 4);
        when(expand.apply(anyString())).thenReturn(farthestTransfers);

        List<Counterparty> result = aggregator.graph(TARGET, nearestTransfers, 2, expand);

        assertThat(result).extracting(Counterparty::address)
                .containsExactly("n0", "n1", "n2", "f0", "f1");
        assertThat(result).extracting(Counterparty::hops).containsExactly(1, 1, 1, 2, 2);
    }

    private static ChainIngestProperties properties(int maxCounterparties, int hop2ExpandTop, int hop2Reserve) {
        ChainIngestProperties properties = mock(ChainIngestProperties.class);
        lenient().when(properties.maxCounterparties()).thenReturn(maxCounterparties);
        lenient().when(properties.hop2ExpandTop()).thenReturn(hop2ExpandTop);
        lenient().when(properties.hop2Reserve()).thenReturn(hop2Reserve);
        return properties;
    }

    private static List<Transfer> descendingValueTransfers(String prefix, int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> transfer(prefix + i, TransferDirection.OUT, (count - i) * 100L))
                .toList();
    }

    private static Transfer transfer(String counterparty, TransferDirection direction, long value) {
        return new Transfer(counterparty, direction, BigInteger.valueOf(value), AT);
    }
}
