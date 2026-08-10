package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.TronGridApi;
import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.client.dto.TransferSample;
import com.riskscoring.chainingest.client.dto.trongrid.TronAccount;
import com.riskscoring.chainingest.client.dto.trongrid.TronTokenInfo;
import com.riskscoring.chainingest.client.dto.trongrid.TronTrc20Transfer;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.mapper.CounterpartyAggregator;
import com.riskscoring.chainingest.mapper.TronTransferMapper;
import com.riskscoring.chainingest.mapper.TronValues;
import com.riskscoring.common.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class TronAddressDataClient implements ChainDataClient {

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

        AccountActivity activity = activity(target);
        TransferSample sample = activity.sample();

        List<Counterparty> counterparties = counterpartyAggregator.graph(
                target, sample.transfers(), properties.tronGrid().maxHops(),
                counterparty -> activity(counterparty).sample().transfers());

        log.info("Fetched {} on {}: {} transfers, {} counterparties (truncated={})",
                target, chain.displayName(), sample.size(), counterparties.size(), sample.truncated());

        return new AddressFacts(snapshot(target, activity), counterparties);
    }

    private AccountActivity activity(String address) {
        List<TronTrc20Transfer> tokenTransfers = tronGridApi.accountTrc20Transfers(address);

        List<Transfer> transfers = Stream.concat(
                        tronTransferMapper.fromNative(tronGridApi.accountTransactions(address), address).stream(),
                        tronTransferMapper.fromTrc20(tokenTransfers, address).stream())
                .toList();

        boolean truncated = tokenTransfers.size() >= properties.tronGrid().pageSize();

        return new AccountActivity(new TransferSample(transfers, truncated), tokenTransfers);
    }

    private AddressSnapshot snapshot(String address, AccountActivity activity) {
        Instant observedAt = Instant.now();
        Optional<TronAccount> account = tronGridApi.account(address);
        TransferSample sample = activity.sample();

        return new AddressSnapshot(
                sample.size(),
                sample.txCount24h(observedAt),
                account.map(TronAccount::balance).map(String::valueOf).orElse(NO_BALANCE),
                tokenBalances(account, activity.tokenTransfers()),
                account.map(TronAccount::createTime).map(values::timestamp).orElse(null),
                sample.lastActivityAt(),
                sample.truncated(),
                observedAt);
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
                .sorted(Comparator.comparing((TokenBalance token) -> new BigDecimal(token.balanceFormatted())).reversed())
                .limit(properties.maxTokenBalances())
                .toList();
    }

    private Optional<TokenBalance> balance(TronTokenInfo token, String rawBalance) {
        return Optional.ofNullable(token)
                .map(known -> new TokenBalance(
                        known.symbol(), values.scaled(rawBalance, values.decimals(known.decimals())), null));
    }

    private record AccountActivity(TransferSample sample, List<TronTrc20Transfer> tokenTransfers) {
    }
}
