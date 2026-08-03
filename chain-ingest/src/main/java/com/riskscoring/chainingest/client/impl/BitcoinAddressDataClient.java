package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.MempoolApi;
import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.client.dto.mempool.MempoolAddressStats;
import com.riskscoring.chainingest.client.dto.mempool.MempoolChainStats;
import com.riskscoring.chainingest.client.dto.mempool.MempoolTransaction;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.mapper.BitcoinTransferMapper;
import com.riskscoring.chainingest.mapper.BitcoinValues;
import com.riskscoring.chainingest.mapper.CounterpartyAggregator;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.Counterparty;
import com.riskscoring.common.model.ScanTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class BitcoinAddressDataClient implements ChainDataClient {

    private static final int FIRST_HOP = 1;
    private static final int SECOND_HOP = 2;
    private static final Duration WINDOW_24H = Duration.ofHours(24);

    private final MempoolApi mempoolApi;
    private final BitcoinTransferMapper bitcoinTransferMapper;
    private final BitcoinValues values;
    private final CounterpartyAggregator counterpartyAggregator;
    private final ChainIngestProperties properties;

    @Override
    public ChainFamily family() {
        return ChainFamily.BITCOIN;
    }

    @Override
    public ScanTarget target() {
        return ScanTarget.ADDRESS;
    }

    @Override
    public AddressFacts fetch(String address, Chain chain) {
        String target = values.address(address);

        List<MempoolTransaction> transactions = mempoolApi.addressTransactions(target);
        List<Transfer> transfers = bitcoinTransferMapper.fromTransactions(transactions, target);

        List<Counterparty> firstHop = counterpartyAggregator.aggregate(transfers, FIRST_HOP);
        List<Counterparty> secondHop = expandSecondHop(firstHop, target);

        List<Counterparty> counterparties = counterpartyAggregator.merge(
                firstHop, secondHop, properties.maxCounterparties(), properties.hop2Reserve());

        log.info("Fetched {} on {}: {} transactions, {} counterparties",
                target, chain.displayName(), transactions.size(), counterparties.size());

        return new AddressFacts(snapshot(target, transactions), counterparties);
    }

    private List<Counterparty> expandSecondHop(List<Counterparty> firstHop, String target) {
        if (properties.mempool().maxHops() < SECOND_HOP || firstHop.isEmpty()) {
            return List.of();
        }

        Set<String> known = new HashSet<>(firstHop.stream().map(Counterparty::address).toList());
        known.add(target);

        List<Transfer> transfers = firstHop.stream()
                .limit(properties.hop2ExpandTop())
                .flatMap(counterparty -> bitcoinTransferMapper.fromTransactions(
                        mempoolApi.addressTransactions(counterparty.address()), counterparty.address()).stream())
                .filter(transfer -> !known.contains(transfer.counterparty()))
                .toList();

        return counterpartyAggregator.aggregate(transfers, SECOND_HOP);
    }

    private AddressSnapshot snapshot(String address, List<MempoolTransaction> transactions) {
        Instant observedAt = Instant.now();
        MempoolAddressStats stats = mempoolApi.addressStats(address);
        MempoolChainStats confirmed = stats.chainStats();

        return new AddressSnapshot(
                confirmed.txCount(),
                txCount24h(transactions, observedAt),
                balanceSatoshi(confirmed),
                List.of(),
                blockTimes(transactions).min(Comparator.naturalOrder()).orElse(null),
                blockTimes(transactions).max(Comparator.naturalOrder()).orElse(null),
                confirmed.txCount() > transactions.size(),
                observedAt);
    }

    private String balanceSatoshi(MempoolChainStats stats) {
        return String.valueOf(stats.fundedTxoSum() - stats.spentTxoSum());
    }

    private Stream<Instant> blockTimes(List<MempoolTransaction> transactions) {
        return transactions.stream()
                .map(transaction -> values.timestamp(transaction.status()))
                .filter(Objects::nonNull);
    }

    private long txCount24h(List<MempoolTransaction> transactions, Instant observedAt) {
        Instant windowStart = observedAt.minus(WINDOW_24H);
        return blockTimes(transactions)
                .filter(at -> at.isAfter(windowStart))
                .count();
    }
}
