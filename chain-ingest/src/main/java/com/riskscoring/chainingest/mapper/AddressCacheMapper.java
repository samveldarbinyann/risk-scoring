package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.entity.AddressCache;
import com.riskscoring.chainingest.entity.CounterpartyCache;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Counterparty;
import com.riskscoring.common.model.TokenBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AddressCacheMapper {

    private static final TypeReference<List<TokenBalance>> TOKEN_BALANCE_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public AddressFacts toFacts(AddressCache cache) {
        AddressSnapshot snapshot = new AddressSnapshot(
                cache.getTxCount(),
                cache.getTxCount24h(),
                cache.getBalanceWei().toString(),
                objectMapper.readValue(cache.getTokenBalances(), TOKEN_BALANCE_LIST),
                cache.getFirstSeenAt(),
                cache.getLastSeenAt(),
                cache.isSampleTruncated(),
                cache.getFetchedAt()
        );

        List<Counterparty> counterparties = cache.getCounterparties().stream()
                .map(counterparty -> new Counterparty(
                        counterparty.getAddress(),
                        counterparty.getDirection(),
                        counterparty.getTxCount(),
                        counterparty.getTotalValueWei().toString(),
                        counterparty.getHops()
                ))
                .toList();

        return new AddressFacts(snapshot, counterparties);
    }

    public AddressCache newEntity(String address, int chainId) {
        return AddressCache.builder()
                .id(UUID.randomUUID())
                .chainId(chainId)
                .address(address)
                .build();
    }

    public void updateSnapshot(AddressCache cache, AddressSnapshot snapshot) {
        cache.setTxCount(snapshot.txCount());
        cache.setTxCount24h(snapshot.txCount24h());
        cache.setBalanceWei(new BigInteger(snapshot.balanceWei()));
        cache.setTokenBalances(objectMapper.writeValueAsString(snapshot.tokenBalances()));
        cache.setFirstSeenAt(snapshot.firstSeenAt());
        cache.setLastSeenAt(snapshot.lastSeenAt());
        cache.setSampleTruncated(snapshot.sampleTruncated());
        cache.setFetchedAt(snapshot.observedAt());
    }

    public List<CounterpartyCache> toEntities(List<Counterparty> counterparties) {
        return counterparties.stream()
                .map(counterparty -> CounterpartyCache.builder()
                        .id(UUID.randomUUID())
                        .address(counterparty.address())
                        .direction(counterparty.direction())
                        .txCount(counterparty.txCount())
                        .totalValueWei(new BigInteger(counterparty.totalValueWei()))
                        .hops(counterparty.hops())
                        .build())
                .toList();
    }
}
