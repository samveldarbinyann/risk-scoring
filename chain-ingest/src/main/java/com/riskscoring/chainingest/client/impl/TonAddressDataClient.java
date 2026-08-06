package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.TonApi;
import com.riskscoring.chainingest.client.dto.TransferSample;
import com.riskscoring.chainingest.client.dto.tonapi.TonAccount;
import com.riskscoring.chainingest.client.dto.tonapi.TonEvent;
import com.riskscoring.chainingest.client.dto.tonapi.TonJettonBalance;
import com.riskscoring.chainingest.client.dto.tonapi.TonPrice;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.mapper.CounterpartyAggregator;
import com.riskscoring.chainingest.mapper.TonTransferMapper;
import com.riskscoring.chainingest.mapper.TonValues;
import com.riskscoring.common.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TonAddressDataClient implements ChainDataClient {

    private static final String NO_BALANCE = "0";
    private static final String USD = "USD";

    private final TonApi tonApi;
    private final TonValues values;
    private final TonTransferMapper tonTransferMapper;
    private final CounterpartyAggregator counterpartyAggregator;
    private final ChainIngestProperties properties;

    @Override
    public ChainFamily family() {
        return ChainFamily.TON;
    }

    @Override
    public ScanTarget target() {
        return ScanTarget.ADDRESS;
    }

    @Override
    public AddressFacts fetch(String address, Chain chain) {
        String target = values.address(address);

        TransferSample sample = fetchTransfers(target);
        List<Counterparty> counterparties = counterpartyAggregator.graph(
                target, sample.transfers(), properties.tonApi().maxHops(),
                counterparty -> fetchTransfers(counterparty).transfers());

        log.info("Fetched {} on {}: {} transfers, {} counterparties (truncated={})",
                target, chain.displayName(), sample.size(), counterparties.size(), sample.truncated());

        return new AddressFacts(snapshot(target, sample), counterparties);
    }

    private TransferSample fetchTransfers(String address) {
        List<TonEvent> events = tonApi.accountEvents(address);

        return new TransferSample(
                tonTransferMapper.fromEvents(events, address),
                events.size() >= properties.tonApi().pageSize());
    }

    private AddressSnapshot snapshot(String address, TransferSample sample) {
        Instant observedAt = Instant.now();
        TonAccount account = tonApi.account(address);
        List<TonJettonBalance> jettons = tonApi.jettons(address);

        return new AddressSnapshot(
                sample.size(),
                sample.txCount24h(observedAt),
                Optional.ofNullable(account.balance()).map(String::valueOf).orElse(NO_BALANCE),
                tokenBalances(jettons),
                firstSeenAt(address),
                sample.lastActivityAt(),
                sample.truncated(),
                observedAt);
    }

    private Instant firstSeenAt(String address) {
        return tonApi.firstEvent(address)
                .map(TonEvent::timestamp)
                .map(values::timestamp)
                .orElse(null);
    }

    private List<TokenBalance> tokenBalances(List<TonJettonBalance> jettons) {
        return jettons.stream()
                .filter(jetton -> jetton.jetton() != null)
                .sorted(Comparator.comparing((TonJettonBalance jetton) -> usdValue(jetton).orElse(0.0)).reversed())
                .limit(properties.maxTokenBalances())
                .map(jetton -> new TokenBalance(
                        jetton.jetton().symbol(), balanceFormatted(jetton), usdValue(jetton).orElse(null)))
                .toList();
    }

    private Optional<Double> usdValue(TonJettonBalance jetton) {
        return Optional.ofNullable(jetton.price())
                .map(TonPrice::prices)
                .map(prices -> prices.get(USD))
                .map(price -> new BigDecimal(balanceFormatted(jetton)).doubleValue() * price);
    }

    private String balanceFormatted(TonJettonBalance jetton) {
        return values.scaled(jetton.balance(), jetton.jetton().decimals());
    }
}
