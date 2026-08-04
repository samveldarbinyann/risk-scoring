package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.TronGridApi;
import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.client.dto.trongrid.TronAccount;
import com.riskscoring.chainingest.client.dto.trongrid.TronTokenInfo;
import com.riskscoring.chainingest.client.dto.trongrid.TronTrc20Transfer;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.mapper.CounterpartyAggregator;
import com.riskscoring.chainingest.mapper.TronTransferMapper;
import com.riskscoring.chainingest.mapper.TronValues;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class TronAddressDataClient implements ChainDataClient {

    private static final Duration WINDOW_24H = Duration.ofHours(24);
    private static final String NO_BALANCE = "0";

    private final TronGridApi tronGridApi;
    private final TronValues values;
    private final TronTransferMapper tronTransferMapper;
    private final CounterpartyAggregator counterpartyAggregator;
    private final ChainIngestProperties properties;

    @Override
    public ChainFamily family() {
        return ChainFamily.TRON;
    }

    @Override
    public ScanTarget target() {
        return ScanTarget.ADDRESS;
    }

    @Override
    public AddressFacts fetch(String address, Chain chain) {
        String target = values.address(address);

        List<TronTrc20Transfer> tokenTransfers = tronGridApi.accountTrc20Transfers(target);
        TransferSample sample = sample(target, tokenTransfers);

        List<Counterparty> firstHop = counterpartyAggregator.aggregate(
                sample.transfers(), CounterpartyAggregator.FIRST_HOP);
        List<Counterparty> secondHop = counterpartyAggregator.expandSecondHop(
                firstHop, target, properties.tronGrid().maxHops(), properties.hop2ExpandTop(),
                counterparty -> sample(counterparty, tronGridApi.accountTrc20Transfers(counterparty)).transfers());

        List<Counterparty> counterparties = counterpartyAggregator.merge(
                firstHop, secondHop, properties.maxCounterparties(), properties.hop2Reserve());

        log.info("Fetched {} on {}: {} transfers, {} counterparties (truncated={})",
                target, chain.displayName(), sample.transfers().size(), counterparties.size(), sample.truncated());

        return new AddressFacts(snapshot(target, sample, tokenTransfers), counterparties);
    }

    private TransferSample sample(String address, List<TronTrc20Transfer> tokenTransfers) {
        List<Transfer> transfers = Stream.concat(
                        tronTransferMapper.fromNative(tronGridApi.accountTransactions(address), address).stream(),
                        tronTransferMapper.fromTrc20(tokenTransfers, address).stream())
                .toList();

        return new TransferSample(transfers, tokenTransfers.size() >= properties.tronGrid().pageSize());
    }

    private AddressSnapshot snapshot(String address,
                                     TransferSample sample,
                                     List<TronTrc20Transfer> tokenTransfers) {
        Instant observedAt = Instant.now();
        Optional<TronAccount> account = tronGridApi.account(address);

        return new AddressSnapshot(
                sample.transfers().size(),
                txCount24h(sample, observedAt),
                account.map(TronAccount::balance).map(String::valueOf).orElse(NO_BALANCE),
                tokenBalances(account, tokenTransfers),
                account.map(TronAccount::createTime).map(values::timestamp).orElse(null),
                transferTimes(sample).max(Comparator.naturalOrder()).orElse(null),
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

    private List<TokenBalance> tokenBalances(Optional<TronAccount> account, List<TronTrc20Transfer> tokenTransfers) {
        Map<String, TronTokenInfo> knownTokens = tokenTransfers.stream()
                .map(TronTrc20Transfer::tokenInfo)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        token -> values.address(token.address()), Function.identity(), (first, ignored) -> first));

        return account.map(TronAccount::trc20).orElseGet(List::of).stream()
                .flatMap(holding -> holding.entrySet().stream())
                .map(holding -> balance(knownTokens.get(values.address(holding.getKey())), holding.getValue()))
                .flatMap(Optional::stream)
                .limit(properties.maxTokenBalances())
                .toList();
    }

    private Optional<TokenBalance> balance(TronTokenInfo token, String rawBalance) {
        return Optional.ofNullable(token)
                .map(known -> new TokenBalance(
                        known.symbol(), values.scaled(rawBalance, known.decimals()), null));
    }

    private record TransferSample(List<Transfer> transfers, boolean truncated) {
    }
}
