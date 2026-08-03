package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.common.model.Counterparty;
import com.riskscoring.common.model.TransferDirection;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class CounterpartyAggregator {

    private static final Comparator<Counterparty> BY_RELEVANCE = Comparator
            .comparingLong(Counterparty::txCount)
            .thenComparing(counterparty -> new BigInteger(counterparty.totalValueNative()))
            .reversed();

    private static final Comparator<Counterparty> BY_PROXIMITY = Comparator
            .comparingInt(Counterparty::hops)
            .thenComparing(BY_RELEVANCE);

    public List<Counterparty> aggregate(List<Transfer> transfers, int hops) {
        Map<String, List<Transfer>> byCounterparty = transfers.stream()
                .collect(Collectors.groupingBy(Transfer::counterparty));

        return byCounterparty.entrySet().stream()
                .map(entry -> toCounterparty(entry.getKey(), entry.getValue(), hops))
                .sorted(BY_RELEVANCE)
                .toList();
    }

    public List<Counterparty> merge(List<Counterparty> nearest, List<Counterparty> farthest, int limit, int reserve) {
        List<Counterparty> near = nearest.stream()
                .limit(limit - Math.min(farthest.size(), reserve))
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
