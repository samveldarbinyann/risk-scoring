package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.ChainData;
import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.MoralisApi;
import com.riskscoring.chainingest.client.dto.MoralisActiveChain;
import com.riskscoring.chainingest.client.dto.MoralisHistoryEnvelope;
import com.riskscoring.chainingest.client.dto.MoralisTokenBalance;
import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.exception.UnsupportedChainException;
import com.riskscoring.chainingest.mapper.CounterpartyAggregator;
import com.riskscoring.chainingest.mapper.TransferMapper;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Counterparty;
import com.riskscoring.common.model.EvmChain;
import com.riskscoring.common.model.TokenBalance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class MoralisChainDataClient implements ChainDataClient {

    private static final int FIRST_HOP = 1;
    private static final int SECOND_HOP = 2;
    private static final Duration WINDOW_24H = Duration.ofHours(24);

    private final MoralisApi moralisApi;
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
        MoralisHistoryEnvelope envelope = moralisApi.walletHistory(address, chainId);
        List<Transfer> transfers = transferMapper.fromTransactions(envelope.result(), address);
        return new TransferSample(transfers, envelope.cursor() != null);
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
        Optional<MoralisActiveChain> activity = moralisApi.walletActivity(address, chainId);

        Instant firstSeenAt = activity.map(MoralisActiveChain::firstTransaction)
                .map(ref -> transferMapper.timestamp(ref.blockTimestamp()))
                .orElse(null);
        Instant lastSeenAt = activity.map(MoralisActiveChain::lastTransaction)
                .map(ref -> transferMapper.timestamp(ref.blockTimestamp()))
                .orElse(null);

        return new AddressSnapshot(
                ageDays(firstSeenAt),
                sample.transfers().size(),
                txCount24h(sample),
                moralisApi.balanceWei(address, chainId),
                tokenBalances(address, chainId),
                firstSeenAt,
                lastSeenAt,
                sample.truncated());
    }

    private long txCount24h(TransferSample sample) {
        Instant windowStart = Instant.now().minus(WINDOW_24H);
        return sample.transfers().stream()
                .filter(transfer -> transfer.at() != null && transfer.at().isAfter(windowStart))
                .count();
    }

    private List<TokenBalance> tokenBalances(String address, int chainId) {
        return moralisApi.tokenBalances(address, chainId).result().stream()
                .filter(token -> !token.possibleSpam())
                .sorted(Comparator.comparing(
                        (MoralisTokenBalance token) -> Optional.ofNullable(token.usdValue()).orElse(0.0)).reversed())
                .limit(properties.maxTokenBalances())
                .map(token -> new TokenBalance(token.symbol(), token.balanceFormatted(), token.usdValue()))
                .toList();
    }

    private int ageDays(Instant firstSeenAt) {
        return Optional.ofNullable(firstSeenAt)
                .map(seen -> (int) Duration.between(seen, Instant.now()).toDays())
                .orElse(0);
    }

    private record TransferSample(List<Transfer> transfers, boolean truncated) {
    }
}
