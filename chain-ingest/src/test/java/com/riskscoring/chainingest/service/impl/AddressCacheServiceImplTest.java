package com.riskscoring.chainingest.service.impl;

import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.entity.AddressCache;
import com.riskscoring.chainingest.entity.CounterpartyCache;
import com.riskscoring.chainingest.mapper.AddressCacheMapper;
import com.riskscoring.chainingest.repository.AddressCacheRepository;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFacts;
import com.riskscoring.common.model.TransactionFacts;
import com.riskscoring.common.model.TransactionSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressCacheServiceImplTest {

    private static final Chain CHAIN = Chain.ETHEREUM;
    private static final String ADDRESS = "0xabc";

    @Mock
    private AddressCacheRepository addressCacheRepository;

    @Mock
    private AddressCacheMapper addressCacheMapper;

    @Mock
    private ChainIngestProperties properties;

    private AddressCacheServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AddressCacheServiceImpl(addressCacheRepository, addressCacheMapper, properties);
        lenient().when(properties.cacheTtl()).thenReturn(Duration.ofHours(1));
    }

    @Test
    void findFreshReturnsMappedFactsWhenCacheRowIsWithinTtl() {
        AddressCache cache = cache(Instant.now().minus(Duration.ofMinutes(10)));
        when(addressCacheRepository.findByChainAndAddress(CHAIN, ADDRESS)).thenReturn(Optional.of(cache));
        AddressFacts facts = new AddressFacts(null, List.of());
        when(addressCacheMapper.toFacts(cache)).thenReturn(facts);

        Optional<ChainFacts> result = service.findFresh(ADDRESS, CHAIN);

        assertThat(result).contains(facts);
    }

    @Test
    void findFreshReturnsEmptyWhenCacheRowIsStale() {
        AddressCache cache = cache(Instant.now().minus(Duration.ofHours(2)));
        when(addressCacheRepository.findByChainAndAddress(CHAIN, ADDRESS)).thenReturn(Optional.of(cache));

        Optional<ChainFacts> result = service.findFresh(ADDRESS, CHAIN);

        assertThat(result).isEmpty();
        verify(addressCacheMapper, never()).toFacts(any());
    }

    @Test
    void findFreshReturnsEmptyWhenNoCacheRow() {
        when(addressCacheRepository.findByChainAndAddress(CHAIN, ADDRESS)).thenReturn(Optional.empty());

        assertThat(service.findFresh(ADDRESS, CHAIN)).isEmpty();
    }

    @Test
    void storeUpdatesExistingCacheRowAndReplacesCounterparties() {
        AddressCache existing = cache(Instant.now());
        when(addressCacheRepository.findByChainAndAddress(CHAIN, ADDRESS)).thenReturn(Optional.of(existing));
        AddressSnapshot snapshot = new AddressSnapshot(1, 1, "100", List.of(), null, null, false, Instant.now());
        List<com.riskscoring.common.model.Counterparty> counterparties = List.of();
        AddressFacts facts = new AddressFacts(snapshot, counterparties);
        List<CounterpartyCache> mappedCounterparties = List.of(CounterpartyCache.builder().build());
        when(addressCacheMapper.toEntities(counterparties)).thenReturn(mappedCounterparties);

        service.store(ADDRESS, CHAIN, facts);

        verify(addressCacheMapper).updateSnapshot(existing, snapshot);
        ArgumentCaptor<AddressCache> savedCaptor = ArgumentCaptor.forClass(AddressCache.class);
        verify(addressCacheRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).isSameAs(existing);
        assertThat(existing.getCounterparties()).isEqualTo(mappedCounterparties);
    }

    @Test
    void storeCreatesNewEntityWhenNoExistingCacheRow() {
        when(addressCacheRepository.findByChainAndAddress(CHAIN, ADDRESS)).thenReturn(Optional.empty());
        AddressCache newEntity = cache(Instant.now());
        when(addressCacheMapper.newEntity(ADDRESS, CHAIN)).thenReturn(newEntity);
        AddressSnapshot snapshot = new AddressSnapshot(1, 1, "100", List.of(), null, null, false, Instant.now());
        AddressFacts facts = new AddressFacts(snapshot, List.of());
        when(addressCacheMapper.toEntities(List.of())).thenReturn(List.of());

        service.store(ADDRESS, CHAIN, facts);

        verify(addressCacheMapper).newEntity(ADDRESS, CHAIN);
        verify(addressCacheRepository).save(newEntity);
    }

    @Test
    void storeThrowsClassCastExceptionWhenFactsAreNotAddressFacts() {
        TransactionFacts transactionFacts = new TransactionFacts(
                new TransactionSnapshot("h", "f", "t", "0", true, Instant.now(), List.of(), 0, 0, List.of(), Instant.now()));

        assertThatThrownBy(() -> service.store(ADDRESS, CHAIN, transactionFacts))
                .isInstanceOf(ClassCastException.class);
    }

    private static AddressCache cache(Instant fetchedAt) {
        return AddressCache.builder()
                .id(UUID.randomUUID())
                .chain(CHAIN)
                .address(ADDRESS)
                .fetchedAt(fetchedAt)
                .build();
    }
}
