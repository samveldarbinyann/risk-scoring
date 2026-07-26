package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.ChainData;
import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.EtherscanApi;
import com.riskscoring.chainingest.client.dto.EtherscanTokenTx;
import com.riskscoring.chainingest.client.dto.EtherscanTx;
import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.exception.UnsupportedChainException;
import com.riskscoring.chainingest.mapper.CounterpartyAggregator;
import com.riskscoring.chainingest.mapper.TransferMapper;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Counterparty;
import com.riskscoring.common.model.EvmChain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class EtherscanChainDataClient implements ChainDataClient {

    private static final int FIRST_HOP = 1;
    private static final int SECOND_HOP = 2;

    private final EtherscanApi etherscanApi;
    private final TransferMapper transferMapper;
    private final CounterpartyAggregator counterpartyAggregator;
    private final ChainIngestProperties properties;

    @Override
    public ChainData fetch(String address, int chainId) {
        EvmChain chain = EvmChain.byId(chainId).orElseThrow(() -> new UnsupportedChainException(chainId));
        String target = address.toLowerCase(Locale.ROOT);

        TransferSample sample = fetchTransfers(target, chainId);
        List<Counterparty> firstHop = counterpartyAggregator.aggregate(sample.transfers(), FIRST_HOP);
        List<Counterparty> secondHop = expandSecondHop(firstHop, target, chainId);

        List<Counterparty> counterparties = counterpartyAggregator.merge(
                firstHop, secondHop, properties.maxCounterparties(), properties.hop2Reserve());

        log.info("Fetched {} on {}: {} transfers, {} counterparties (truncated={})",
                target, chain.displayName(), sample.transfers().size(), counterparties.size(), sample.truncated());

        return new ChainData(snapshot(target, chainId, sample), counterparties);
    }

    private TransferSample fetchTransfers(String address, int chainId) {
        List<EtherscanTx> transactions = etherscanApi.latestTransactions(address, chainId);
        List<EtherscanTx> internal = etherscanApi.latestInternalTransactions(address, chainId);
        List<EtherscanTokenTx> tokenTransfers = etherscanApi.latestTokenTransfers(address, chainId);

        List<Transfer> transfers = Stream.of(
                        transferMapper.fromTransactions(transactions, address),
                        transferMapper.fromTransactions(internal, address),
                        transferMapper.fromTokenTransfers(tokenTransfers, address))
                .flatMap(List::stream)
                .toList();

        boolean truncated = Stream.of(transactions.size(), internal.size(), tokenTransfers.size())
                .anyMatch(size -> size >= properties.etherscan().pageSize());

        return new TransferSample(transfers, truncated);
    }

    private List<Counterparty> expandSecondHop(List<Counterparty> firstHop, String target, int chainId) {
        if (properties.maxHops() < SECOND_HOP || firstHop.isEmpty()) {
            return List.of();
        }

        Set<String> known = new HashSet<>(firstHop.stream().map(Counterparty::address).toList());
        known.add(target);

        List<Transfer> transfers = firstHop.stream()
                .limit(properties.hop2ExpandTop())
                .flatMap(counterparty -> fetchTransfers(counterparty.address(), chainId).transfers().stream())
                .filter(transfer -> !known.contains(transfer.counterparty()))
                .toList();

        return counterpartyAggregator.aggregate(transfers, SECOND_HOP);
    }

    private AddressSnapshot snapshot(String address, int chainId, TransferSample sample) {
        Instant firstSeenAt = firstSeenAt(address, chainId, sample);
        Instant lastSeenAt = boundary(sample.transfers(), Comparator.reverseOrder());

        return new AddressSnapshot(
                ageDays(firstSeenAt),
                sample.transfers().size(),
                etherscanApi.balanceWei(address, chainId),
                firstSeenAt,
                lastSeenAt,
                sample.truncated());
    }

    private Instant firstSeenAt(String address, int chainId, TransferSample sample) {
        return etherscanApi.firstTransaction(address, chainId)
                .map(transaction -> transferMapper.timestamp(transaction.timeStamp()))
                .orElseGet(() -> boundary(sample.transfers(), Comparator.naturalOrder()));
    }

    private Instant boundary(List<Transfer> transfers, Comparator<Instant> order) {
        return transfers.stream()
                .map(Transfer::at)
                .filter(Objects::nonNull)
                .min(order)
                .orElse(null);
    }

    private int ageDays(Instant firstSeenAt) {
        return Optional.ofNullable(firstSeenAt)
                .map(seen -> (int) Duration.between(seen, Instant.now()).toDays())
                .orElse(0);
    }

    private record TransferSample(List<Transfer> transfers, boolean truncated) {
    }
}
