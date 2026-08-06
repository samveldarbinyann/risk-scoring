package com.riskscoring.chainingest.service.impl;

import com.riskscoring.chainingest.entity.TransactionCache;
import com.riskscoring.chainingest.mapper.TransactionCacheMapper;
import com.riskscoring.chainingest.repository.TransactionCacheRepository;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFacts;
import com.riskscoring.common.model.TransactionFacts;
import com.riskscoring.common.model.TransactionSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionCacheServiceImplTest {

    private static final Chain CHAIN = Chain.BITCOIN;
    private static final String TX_HASH = "hash1";

    @Mock
    private TransactionCacheRepository transactionCacheRepository;

    @Mock
    private TransactionCacheMapper transactionCacheMapper;

    private TransactionCacheServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TransactionCacheServiceImpl(transactionCacheRepository, transactionCacheMapper);
    }

    @Test
    void findFreshReturnsCachedFactsRegardlessOfAge() {
        TransactionCache cache = TransactionCache.builder()
                .id(UUID.randomUUID())
                .fetchedAt(Instant.now().minus(365, ChronoUnit.DAYS))
                .build();
        when(transactionCacheRepository.findByChainAndTxHash(CHAIN, TX_HASH)).thenReturn(Optional.of(cache));
        TransactionSnapshot snapshot = snapshot(Instant.now());
        when(transactionCacheMapper.toSnapshot(cache)).thenReturn(snapshot);

        Optional<ChainFacts> result = service.findFresh(TX_HASH, CHAIN);

        assertThat(result).contains(new TransactionFacts(snapshot));
    }

    @Test
    void findFreshReturnsEmptyWhenNoCacheRow() {
        when(transactionCacheRepository.findByChainAndTxHash(CHAIN, TX_HASH)).thenReturn(Optional.empty());

        assertThat(service.findFresh(TX_HASH, CHAIN)).isEmpty();
    }

    @Test
    void storeSkipsCachingPendingTransactionWithNullBlockTimestamp() {
        TransactionFacts facts = new TransactionFacts(snapshot(null));

        service.store(TX_HASH, CHAIN, facts);

        verify(transactionCacheRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void storeSavesNewEntityWhenNotAlreadyCached() {
        when(transactionCacheRepository.findByChainAndTxHash(CHAIN, TX_HASH)).thenReturn(Optional.empty());
        TransactionSnapshot snapshot = snapshot(Instant.now());
        TransactionFacts facts = new TransactionFacts(snapshot);
        TransactionCache entity = TransactionCache.builder().id(UUID.randomUUID()).build();
        when(transactionCacheMapper.toEntity(CHAIN, snapshot)).thenReturn(entity);

        service.store(TX_HASH, CHAIN, facts);

        verify(transactionCacheRepository).save(entity);
    }

    @Test
    void storeIsIdempotentWhenTransactionAlreadyCached() {
        TransactionCache existing = TransactionCache.builder().id(UUID.randomUUID()).build();
        when(transactionCacheRepository.findByChainAndTxHash(CHAIN, TX_HASH)).thenReturn(Optional.of(existing));
        TransactionFacts facts = new TransactionFacts(snapshot(Instant.now()));

        service.store(TX_HASH, CHAIN, facts);

        verify(transactionCacheRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private static TransactionSnapshot snapshot(Instant blockTimestamp) {
        return new TransactionSnapshot(TX_HASH, "from", "to", "0", true, blockTimestamp, List.of(), 0, 0, List.of(), Instant.now());
    }
}
