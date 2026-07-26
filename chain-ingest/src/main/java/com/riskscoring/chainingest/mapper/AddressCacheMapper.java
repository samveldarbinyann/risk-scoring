package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.ChainData;
import com.riskscoring.chainingest.entity.AddressCache;
import com.riskscoring.chainingest.entity.CounterpartyCache;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Counterparty;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class AddressCacheMapper {

    public ChainData toChainData(AddressCache cache) {
        AddressSnapshot snapshot = new AddressSnapshot(
                cache.getAgeDays(),
                cache.getTxCount(),
                cache.getBalanceWei().toString(),
                cache.getFirstSeenAt(),
                cache.getLastSeenAt()
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

        return new ChainData(snapshot, counterparties);
    }

    public AddressCache newEntity(String address, int chainId) {
        return AddressCache.builder()
                .id(UUID.randomUUID())
                .chainId(chainId)
                .address(address)
                .build();
    }

    public void updateSnapshot(AddressCache cache, AddressSnapshot snapshot, Instant fetchedAt) {
        cache.setAgeDays(snapshot.ageDays());
        cache.setTxCount(snapshot.txCount());
        cache.setBalanceWei(new BigInteger(snapshot.balanceWei()));
        cache.setFirstSeenAt(snapshot.firstSeenAt());
        cache.setLastSeenAt(snapshot.lastSeenAt());
        cache.setFetchedAt(fetchedAt);
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
