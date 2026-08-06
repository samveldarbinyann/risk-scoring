package com.riskscoring.monitor.service.impl;

import com.riskscoring.common.event.WatchlistAddRequested;
import com.riskscoring.common.event.WatchlistRemoveRequested;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
import com.riskscoring.monitor.entity.WatchlistEntry;
import com.riskscoring.monitor.mapper.WatchlistMapper;
import com.riskscoring.monitor.repository.WatchlistEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceImplTest {

    private static final Chain CHAIN = Chain.ETHEREUM;
    private static final String ADDRESS = "0xabc";

    @Mock
    private WatchlistEntryRepository watchlistEntryRepository;
    @Mock
    private WatchlistMapper watchlistMapper;

    private WatchlistServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WatchlistServiceImpl(watchlistEntryRepository, watchlistMapper);
    }

    @Test
    void addEntryReactivatesExistingEntryWithoutExplicitSave() {
        UUID userId = UUID.randomUUID();
        WatchlistEntry existing = watchlistEntry(userId, false, Language.EN);
        WatchlistAddRequested event = addRequest(userId, Language.RU);
        when(watchlistEntryRepository.findByUserIdAndChainAndAddress(userId, CHAIN, ADDRESS))
                .thenReturn(Optional.of(existing));

        service.addEntry(event);

        assertThat(existing.isActive()).isTrue();
        assertThat(existing.getLanguage()).isEqualTo(Language.RU);
        assertThat(existing.getUpdatedAt()).isNotNull();
        verify(watchlistEntryRepository, never()).save(any());
        verifyNoInteractions(watchlistMapper);
    }

    @Test
    void addEntryCreatesAndSavesNewEntryWhenNoneExists() {
        UUID userId = UUID.randomUUID();
        WatchlistAddRequested event = addRequest(userId, Language.EN);
        WatchlistEntry newEntry = watchlistEntry(userId, true, Language.EN);
        when(watchlistEntryRepository.findByUserIdAndChainAndAddress(userId, CHAIN, ADDRESS))
                .thenReturn(Optional.empty());
        when(watchlistMapper.toEntity(eq(event), any())).thenReturn(newEntry);
        when(watchlistEntryRepository.save(newEntry)).thenReturn(newEntry);

        service.addEntry(event);

        verify(watchlistEntryRepository).save(newEntry);
    }

    @Test
    void removeEntryDeactivatesEntryFoundForUser() {
        UUID userId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        WatchlistEntry entry = watchlistEntry(userId, true, Language.EN);
        WatchlistRemoveRequested event = new WatchlistRemoveRequested(UUID.randomUUID(), userId, entryId, Instant.now());
        when(watchlistEntryRepository.findByIdAndUserId(entryId, userId)).thenReturn(Optional.of(entry));

        service.removeEntry(event);

        assertThat(entry.isActive()).isFalse();
        assertThat(entry.getUpdatedAt()).isNotNull();
        verify(watchlistEntryRepository, never()).save(any());
    }

    @Test
    void removeEntryDoesNothingWhenEntryNotFoundOrNotOwnedByUser() {
        UUID userId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        WatchlistRemoveRequested event = new WatchlistRemoveRequested(UUID.randomUUID(), userId, entryId, Instant.now());
        when(watchlistEntryRepository.findByIdAndUserId(entryId, userId)).thenReturn(Optional.empty());

        service.removeEntry(event);

        verify(watchlistEntryRepository, never()).save(any());
    }

    private static WatchlistAddRequested addRequest(UUID userId, Language language) {
        return new WatchlistAddRequested(UUID.randomUUID(), userId, ADDRESS, CHAIN, language, Instant.now());
    }

    private static WatchlistEntry watchlistEntry(UUID userId, boolean active, Language language) {
        return WatchlistEntry.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .address(ADDRESS)
                .chain(CHAIN)
                .language(language)
                .active(active)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
