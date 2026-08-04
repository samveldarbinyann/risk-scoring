package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.common.model.Counterparty;
import com.riskscoring.common.model.TransferDirection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class CounterpartyAggregator {

    private static final int FIRST_HOP = 1;
    private static final int SECOND_HOP = 2;

    private static final Comparator<Counterparty> BY_RELEVANCE = Comparator
            .comparingLong(Counterparty::txCount)
            .thenComparing(counterparty -> new BigInteger(counterparty.totalValueNative()))
            .reversed();

    private static final Comparator<Counterparty> BY_PROXIMITY = Comparator
            .comparingInt(Counterparty::hops)
            .thenComparing(BY_RELEVANCE);

    private final ChainIngestProperties properties;

    public List<Counterparty> graph(String target, List<Transfer> transfers, int maxHops,
                                    Function<String, List<Transfer>> expand) {
        List<Counterparty> firstHop = aggregate(transfers, FIRST_HOP);
        List<Counterparty> secondHop = expandSecondHop(firstHop, target, maxHops, expand);

        return merge(firstHop, secondHop);
    }

    private List<Counterparty> aggregate(List<Transfer> transfers, int hops) {
        Map<String, List<Transfer>> byCounterparty = transfers.stream()
                .collect(Collectors.groupingBy(Transfer::counterparty));

        return byCounterparty.entrySet().stream()
                .map(entry -> toCounterparty(entry.getKey(), entry.getValue(), hops))
                .sorted(BY_RELEVANCE)
                .toList();
    }

    private List<Counterparty> expandSecondHop(List<Counterparty> firstHop,
                                               String target,
                                               int maxHops,
                                               Function<String, List<Transfer>> fetch) {
        if (maxHops < SECOND_HOP || firstHop.isEmpty()) {
            return List.of();
        }

        Set<String> known = new HashSet<>(firstHop.stream().map(Counterparty::address).toList());
        known.add(target);

        List<Transfer> transfers = firstHop.stream()
                .limit(properties.hop2ExpandTop())
                .flatMap(counterparty -> fetch.apply(counterparty.address()).stream())
                .filter(transfer -> !known.contains(transfer.counterparty()))
                .toList();

        return aggregate(transfers, SECOND_HOP);
    }

    private List<Counterparty> merge(List<Counterparty> nearest, List<Counterparty> farthest) {
        int limit = properties.maxCounterparties();

        List<Counterparty> near = nearest.stream()
                .limit(limit - Math.min(farthest.size(), properties.hop2Reserve()))
                .toList();

        List<Counterparty> far = farthest.stream()
                .limit(limit - near.size())
                .toList();

        return Stream.concat(near.stream(), far.stream())
                .sorted(BY_PROXIMITY)
                .toList();
    }

    private Counterparty toCounterparty(String address, List<Transfer> transfers, int hops) {
        BigInteger totalValueNative = transfers.stream()
                .map(Transfer::valueNative)
                .reduce(BigInteger.ZERO, BigInteger::add);

        return new Counterparty(address, direction(transfers), transfers.size(), totalValueNative.toString(), hops);
    }

    private TransferDirection direction(List<Transfer> transfers) {
        boolean incoming = transfers.stream().anyMatch(transfer -> transfer.direction() == TransferDirection.IN);
        boolean outgoing = transfers.stream().anyMatch(transfer -> transfer.direction() == TransferDirection.OUT);

        if (incoming && outgoing) {
            return TransferDirection.BOTH;
        }

        return incoming ? TransferDirection.IN : TransferDirection.OUT;
    }
}
