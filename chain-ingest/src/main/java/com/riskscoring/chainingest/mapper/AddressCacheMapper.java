package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.entity.AddressCache;
import com.riskscoring.chainingest.entity.CachedCounterparty;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Counterparty;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class AddressCacheMapper {

    public AddressSnapshot toSnapshot(AddressCache cache) {
        return new AddressSnapshot(
                cache.getAgeDays(),
                cache.getTxCount(),
                cache.getBalanceWei().toString(),
                cache.getFirstSeenAt(),
                cache.getLastSeenAt()
        );
    }

    public List<Counterparty> toCounterparties(AddressCache cache) {
        return cache.getCounterparties().stream()
                .map(counterparty -> new Counterparty(
                        counterparty.getAddress(),
                        counterparty.getDirection(),
                        counterparty.getTxCount(),
                        counterparty.getTotalValueWei().toString(),
                        counterparty.getHops()
                ))
                .toList();
    }

    public AddressCache toEntity(AddressSnapshot snapshot, String address, int chainId, Instant fetchedAt) {
        return AddressCache.builder()
                .id(UUID.randomUUID())
                .chainId(chainId)
                .address(address)
                .ageDays(snapshot.ageDays())
                .txCount(snapshot.txCount())
                .balanceWei(new BigInteger(snapshot.balanceWei()))
                .firstSeenAt(snapshot.firstSeenAt())
                .lastSeenAt(snapshot.lastSeenAt())
                .fetchedAt(fetchedAt)
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

    public List<CachedCounterparty> toEntities(List<Counterparty> counterparties) {
        return counterparties.stream()
                .map(counterparty -> CachedCounterparty.builder()
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
