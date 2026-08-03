package com.riskscoring.chainingest.client.impl;

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
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Counterparty;
import com.riskscoring.common.model.EvmChain;
import com.riskscoring.common.model.ScanTarget;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class MoralisAddressDataClient implements ChainDataClient {

    private static final int FIRST_HOP = 1;
    private static final int SECOND_HOP = 2;
    private static final Duration WINDOW_24H = Duration.ofHours(24);

    private final MoralisApi moralisApi;
    private final TransferMapper transferMapper;
    private final CounterpartyAggregator counterpartyAggregator;
    private final ChainIngestProperties properties;

    @Override
    public ScanTarget target() {
        return ScanTarget.ADDRESS;
    }

    @Override
    public AddressFacts fetch(String address, int chainId) {
        EvmChain chain = EvmChain.byId(chainId).orElseThrow(() -> new UnsupportedChainException(chainId));
        String target = address.toLowerCase(Locale.ROOT);

        TransferSample sample = fetchTransfers(target, chainId);
        List<Counterparty> firstHop = counterpartyAggregator.aggregate(sample.transfers(), FIRST_HOP);
        List<Counterparty> secondHop = expandSecondHop(firstHop, target, chainId);

        List<Counterparty> counterparties = counterpartyAggregator.merge(
                firstHop, secondHop, properties.maxCounterparties(), properties.hop2Reserve());

        log.info("Fetched {} on {}: {} transfers, {} counterparties (truncated={})",
                target, chain.displayName(), sample.transfers().size(), counterparties.size(), sample.truncated());

        return new AddressFacts(snapshot(target, chainId, sample), counterparties);
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
        Instant observedAt = Instant.now();
        Optional<MoralisActiveChain> activity = moralisApi.walletActivity(address, chainId);

        Instant firstSeenAt = activity.map(MoralisActiveChain::firstTransaction)
                .map(ref -> transferMapper.timestamp(ref.blockTimestamp()))
                .orElseGet(() -> transferTimes(sample).min(Comparator.naturalOrder()).orElse(null));
        Instant lastSeenAt = activity.map(MoralisActiveChain::lastTransaction)
                .map(ref -> transferMapper.timestamp(ref.blockTimestamp()))
                .orElseGet(() -> transferTimes(sample).max(Comparator.naturalOrder()).orElse(null));

        return new AddressSnapshot(
                sample.transfers().size(),
                txCount24h(sample, observedAt),
                moralisApi.balanceWei(address, chainId),
                tokenBalances(address, chainId),
                firstSeenAt,
                lastSeenAt,
                sample.truncated(),
                observedAt);
    }

    private Stream<Instant> transferTimes(TransferSample sample) {
        return sample.transfers().stream()
                .map(Transfer::at)
                .filter(Objects::nonNull);
    }

    private long txCount24h(TransferSample sample, Instant observedAt) {
        Instant windowStart = observedAt.minus(WINDOW_24H);
        return transferTimes(sample)
                .filter(at -> at.isAfter(windowStart))
                .count();
    }

    private List<TokenBalance> tokenBalances(String address, int chainId) {
        return moralisApi.tokenBalances(address, chainId).stream()
                .sorted(Comparator.comparing(
                        (MoralisTokenBalance token) -> Optional.ofNullable(token.usdValue()).orElse(0.0)).reversed())
                .limit(properties.maxTokenBalances())
                .map(token -> new TokenBalance(token.symbol(), token.balanceFormatted(), token.usdValue()))
                .toList();
    }

    private record TransferSample(List<Transfer> transfers, boolean truncated) {
    }
}
