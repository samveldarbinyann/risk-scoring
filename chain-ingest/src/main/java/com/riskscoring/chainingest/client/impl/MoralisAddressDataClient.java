package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.MoralisApi;
import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.client.dto.TransferSample;
import com.riskscoring.chainingest.client.dto.moralis.MoralisActiveChain;
import com.riskscoring.chainingest.client.dto.moralis.MoralisHistoryEnvelope;
import com.riskscoring.chainingest.client.dto.moralis.MoralisTokenBalance;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.exception.ChainDataException;
import com.riskscoring.chainingest.mapper.CounterpartyAggregator;
import com.riskscoring.chainingest.mapper.MoralisValues;
import com.riskscoring.chainingest.mapper.TransferMapper;
import com.riskscoring.common.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MoralisAddressDataClient implements ChainDataClient {

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
        List<Counterparty> counterparties = counterpartyAggregator.graph(
                target, sample.transfers(), properties.moralis().maxHops(),
                counterparty -> fetchTransfers(counterparty, chain).transfers());

        log.info("Fetched {} on {}: {} transfers, {} counterparties (truncated={})",
                target, chain.displayName(), sample.size(), counterparties.size(), sample.truncated());

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
                .orElseGet(sample::lastActivityAt);

        return new AddressSnapshot(
                sample.size(),
                sample.txCount24h(observedAt),
                moralisApi.balanceNative(address, chain),
                tokenBalances(address, chain),
                lifetimeFirstSeenAt.orElse(null),
                lastSeenAt,
                sample.truncated(),
                observedAt);
    }

    private List<TokenBalance> tokenBalances(String address, Chain chain) {
        try {
            return moralisApi.tokenBalances(address, chain).stream()
                    .sorted(Comparator.comparing(
                            (MoralisTokenBalance token) -> Optional.ofNullable(token.usdValue()).orElse(0.0)).reversed())
                    .limit(properties.maxTokenBalances())
                    .map(token -> new TokenBalance(token.symbol(), token.balanceFormatted(), token.usdValue()))
                    .toList();
        } catch (ChainDataException e) {
            log.error("Error fetching token balances for address {}: {}", address, e.getMessage());
            return List.of();
        }
    }
}
