package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.MoralisApi;
import com.riskscoring.chainingest.client.dto.MoralisActiveChain;
import com.riskscoring.chainingest.client.dto.MoralisHistoryEnvelope;
import com.riskscoring.chainingest.client.dto.MoralisTokenBalance;
import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.mapper.CounterpartyAggregator;
import com.riskscoring.chainingest.mapper.MoralisValues;
import com.riskscoring.chainingest.mapper.TransferMapper;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.Counterparty;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.TokenBalance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class MoralisAddressDataClient implements ChainDataClient {

    private static final Duration WINDOW_24H = Duration.ofHours(24);

    private final MoralisApi moralisApi;
    private final MoralisValues values;
    private final TransferMapper transferMapper;
    private final CounterpartyAggregator counterpartyAggregator;
    private final ChainIngestProperties properties;

    @Override
    public ChainFamily family() {
        return ChainFamily.EVM;
    }

    @Override
    public ScanTarget target() {
        return ScanTarget.ADDRESS;
    }

    @Override
    public AddressFacts fetch(String address, Chain chain) {
        String target = values.address(address);

        TransferSample sample = fetchTransfers(target, chain);
        List<Counterparty> firstHop = counterpartyAggregator.aggregate(
                sample.transfers(), CounterpartyAggregator.FIRST_HOP);
        List<Counterparty> secondHop = counterpartyAggregator.expandSecondHop(
                firstHop, target, properties.maxHops(), properties.hop2ExpandTop(),
                counterparty -> fetchTransfers(counterparty, chain).transfers());

        List<Counterparty> counterparties = counterpartyAggregator.merge(
                firstHop, secondHop, properties.maxCounterparties(), properties.hop2Reserve());

        log.info("Fetched {} on {}: {} transfers, {} counterparties (truncated={})",
                target, chain.displayName(), sample.transfers().size(), counterparties.size(), sample.truncated());

        return new AddressFacts(snapshot(target, chain, sample), counterparties);
    }

    private TransferSample fetchTransfers(String address, Chain chain) {
        MoralisHistoryEnvelope envelope = moralisApi.walletHistory(address, chain);
        List<Transfer> transfers = transferMapper.fromTransactions(envelope.result(), address);
        return new TransferSample(transfers, envelope.cursor() != null);
    }

    private AddressSnapshot snapshot(String address, Chain chain, TransferSample sample) {
        Instant observedAt = Instant.now();
        Optional<MoralisActiveChain> activity = moralisApi.walletActivity(address, chain);

        Optional<Instant> lifetimeFirstSeenAt = activity.map(MoralisActiveChain::firstTransaction)
                .map(ref -> transferMapper.timestamp(ref.blockTimestamp()));
        Instant lastSeenAt = activity.map(MoralisActiveChain::lastTransaction)
                .map(ref -> transferMapper.timestamp(ref.blockTimestamp()))
                .orElseGet(() -> transferTimes(sample).max(Comparator.naturalOrder()).orElse(null));

        return new AddressSnapshot(
                sample.transfers().size(),
                txCount24h(sample, observedAt),
                moralisApi.balanceNative(address, chain),
                tokenBalances(address, chain),
                lifetimeFirstSeenAt.orElse(null),
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

    private List<TokenBalance> tokenBalances(String address, Chain chain) {
        return moralisApi.tokenBalances(address, chain).stream()
                .sorted(Comparator.comparing(
                        (MoralisTokenBalance token) -> Optional.ofNullable(token.usdValue()).orElse(0.0)).reversed())
                .limit(properties.maxTokenBalances())
                .map(token -> new TokenBalance(token.symbol(), token.balanceFormatted(), token.usdValue()))
                .toList();
    }

    private record TransferSample(List<Transfer> transfers, boolean truncated) {
    }
}
