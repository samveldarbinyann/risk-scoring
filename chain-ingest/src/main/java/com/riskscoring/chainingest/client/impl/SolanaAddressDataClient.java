package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.HeliusApi;
import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.client.dto.helius.HeliusAsset;
import com.riskscoring.chainingest.client.dto.helius.HeliusNativeBalance;
import com.riskscoring.chainingest.client.dto.helius.HeliusPortfolio;
import com.riskscoring.chainingest.client.dto.helius.HeliusPriceInfo;
import com.riskscoring.chainingest.client.dto.helius.HeliusTokenInfo;
import com.riskscoring.chainingest.client.dto.helius.HeliusTransaction;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.mapper.CounterpartyAggregator;
import com.riskscoring.chainingest.mapper.SolanaTransferMapper;
import com.riskscoring.chainingest.mapper.SolanaValues;
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

import java.math.BigDecimal;
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
public class SolanaAddressDataClient implements ChainDataClient {

    private static final Duration WINDOW_24H = Duration.ofHours(24);
    private static final String NO_BALANCE = "0";

    private final HeliusApi heliusApi;
    private final SolanaValues values;
    private final SolanaTransferMapper solanaTransferMapper;
    private final CounterpartyAggregator counterpartyAggregator;
    private final ChainIngestProperties properties;

    @Override
    public ChainFamily family() {
        return ChainFamily.SOLANA;
    }

    @Override
    public ScanTarget target() {
        return ScanTarget.ADDRESS;
    }

    @Override
    public AddressFacts fetch(String address, Chain chain) {
        String target = values.normalize(address);

        TransferSample sample = fetchTransfers(target);
        List<Counterparty> firstHop = counterpartyAggregator.aggregate(
                sample.transfers(), CounterpartyAggregator.FIRST_HOP);
        List<Counterparty> secondHop = counterpartyAggregator.expandSecondHop(
                firstHop, target, properties.helius().maxHops(), properties.hop2ExpandTop(),
                counterparty -> fetchTransfers(counterparty).transfers());

        List<Counterparty> counterparties = counterpartyAggregator.merge(
                firstHop, secondHop, properties.maxCounterparties(), properties.hop2Reserve());

        log.info("Fetched {} on {}: {} transfers, {} counterparties (truncated={})",
                target, chain.displayName(), sample.transfers().size(), counterparties.size(), sample.truncated());

        return new AddressFacts(snapshot(target, sample), counterparties);
    }

    private TransferSample fetchTransfers(String address) {
        List<HeliusTransaction> transactions = heliusApi.addressTransactions(address);

        return new TransferSample(
                solanaTransferMapper.fromTransactions(transactions, address),
                transactions.size() >= properties.helius().pageSize());
    }

    private AddressSnapshot snapshot(String address, TransferSample sample) {
        Instant observedAt = Instant.now();
        HeliusPortfolio portfolio = heliusApi.portfolio(address);

        return new AddressSnapshot(
                sample.transfers().size(),
                txCount24h(sample, observedAt),
                balanceLamports(portfolio),
                tokenBalances(portfolio),
                null,
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

    private String balanceLamports(HeliusPortfolio portfolio) {
        return Optional.ofNullable(portfolio.nativeBalance())
                .map(HeliusNativeBalance::lamports)
                .map(String::valueOf)
                .orElse(NO_BALANCE);
    }

    private List<TokenBalance> tokenBalances(HeliusPortfolio portfolio) {
        return Optional.ofNullable(portfolio.items()).orElseGet(List::of).stream()
                .map(HeliusAsset::tokenInfo)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing((HeliusTokenInfo token) -> usdValue(token).orElse(0.0)).reversed())
                .limit(properties.maxTokenBalances())
                .map(token -> new TokenBalance(
                        token.symbol(), balanceFormatted(token), usdValue(token).orElse(null)))
                .toList();
    }

    private Optional<Double> usdValue(HeliusTokenInfo token) {
        return Optional.ofNullable(token.priceInfo()).map(HeliusPriceInfo::totalPrice);
    }

    private String balanceFormatted(HeliusTokenInfo token) {
        return Optional.ofNullable(token.balance())
                .filter(balance -> !balance.isBlank())
                .map(balance -> new BigDecimal(balance).movePointLeft(token.decimals()).toPlainString())
                .orElse(NO_BALANCE);
    }

    private record TransferSample(List<Transfer> transfers, boolean truncated) {
    }
}