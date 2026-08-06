package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.event.WatchlistAddRequested;
import com.riskscoring.common.event.WatchlistRemoveRequested;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.gateway.dto.WatchlistCreateRequest;
import com.riskscoring.gateway.dto.WatchlistEntryView;
import com.riskscoring.gateway.exception.TargetChainMismatchException;
import com.riskscoring.gateway.exception.UnsupportedChainException;
import com.riskscoring.gateway.exception.WatchlistEntryNotFoundException;
import com.riskscoring.gateway.kafka.WatchlistEventPublisher;
import com.riskscoring.gateway.mapper.WatchlistMapper;
import com.riskscoring.gateway.repository.WatchlistEntryRow;
import com.riskscoring.gateway.repository.WatchlistRepository;
import com.riskscoring.gateway.service.ChainService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private WatchlistRepository watchlistRepository;
    @Mock
    private WatchlistEventPublisher watchlistEventPublisher;
    @Mock
    private ChainService chainService;

    @Captor
    private ArgumentCaptor<WatchlistAddRequested> addCaptor;

    private WatchlistServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WatchlistServiceImpl(watchlistRepository, watchlistEventPublisher, new WatchlistMapper(), chainService);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void addToWatchlistPublishesEventWithNormalizedAddressAndResolvedChain() {
        String mixedCaseAddress = "0x" + "A".repeat(40);
        when(chainService.requireScannable("ETHEREUM")).thenReturn(Chain.ETHEREUM);

        service.addToWatchlist(USER_ID, new WatchlistCreateRequest(mixedCaseAddress, "ETHEREUM"));

        verify(watchlistEventPublisher).publishWatchlistAddRequested(addCaptor.capture());
        WatchlistAddRequested event = addCaptor.getValue();
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.address()).isEqualTo("0x" + "a".repeat(40));
        assertThat(event.chain()).isEqualTo(Chain.ETHEREUM);
        assertThat(event.language()).isEqualTo(Language.EN);
    }

    @Test
    void addToWatchlistUsesRussianLanguageWhenLocaleIsRussian() {
        LocaleContextHolder.setLocale(Locale.of("ru"));
        String address = "0x" + "a".repeat(40);
        when(chainService.requireScannable("ETHEREUM")).thenReturn(Chain.ETHEREUM);

        service.addToWatchlist(USER_ID, new WatchlistCreateRequest(address, "ETHEREUM"));

        verify(watchlistEventPublisher).publishWatchlistAddRequested(addCaptor.capture());
        assertThat(addCaptor.getValue().language()).isEqualTo(Language.RU);
    }

    @Test
    void addToWatchlistPropagatesUnsupportedChainExceptionWithoutPublishing() {
        when(chainService.requireScannable("NOT_A_CHAIN")).thenThrow(new UnsupportedChainException("NOT_A_CHAIN"));

        assertThatThrownBy(() -> service.addToWatchlist(USER_ID, new WatchlistCreateRequest("0xabc", "NOT_A_CHAIN")))
                .isInstanceOf(UnsupportedChainException.class);

        verifyNoInteractions(watchlistEventPublisher);
    }

    @Test
    void addToWatchlistThrowsTargetChainMismatchExceptionWhenAddressDoesNotMatchChainFamily() {
        when(chainService.requireScannable("ETHEREUM")).thenReturn(Chain.ETHEREUM);

        assertThatThrownBy(() -> service.addToWatchlist(USER_ID, new WatchlistCreateRequest("not-an-evm-address", "ETHEREUM")))
                .isInstanceOf(TargetChainMismatchException.class);

        verifyNoInteractions(watchlistEventPublisher);
    }

    @Test
    void removeFromWatchlistThrowsWatchlistEntryNotFoundExceptionWhenNotOwned() {
        UUID entryId = UUID.randomUUID();
        when(watchlistRepository.existsByIdAndUserId(entryId, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.removeFromWatchlist(USER_ID, entryId))
                .isInstanceOf(WatchlistEntryNotFoundException.class);

        verifyNoInteractions(watchlistEventPublisher);
    }

    @Test
    void removeFromWatchlistPublishesRemoveEventWhenEntryIsOwnedByUser() {
        UUID entryId = UUID.randomUUID();
        when(watchlistRepository.existsByIdAndUserId(entryId, USER_ID)).thenReturn(true);

        service.removeFromWatchlist(USER_ID, entryId);

        ArgumentCaptor<WatchlistRemoveRequested> captor = ArgumentCaptor.forClass(WatchlistRemoveRequested.class);
        verify(watchlistEventPublisher).publishWatchlistRemoveRequested(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().entryId()).isEqualTo(entryId);
    }

    @Test
    void listWatchlistMapsEachRowToAView() {
        WatchlistEntryRow row = new WatchlistEntryRow(UUID.randomUUID(), "0xabc", Chain.ETHEREUM,
                RiskLevel.LOW, 10, UUID.randomUUID(), Instant.now(), Instant.now());
        when(watchlistRepository.findAllByUserId(USER_ID)).thenReturn(List.of(row));

        List<WatchlistEntryView> views = service.listWatchlist(USER_ID);

        assertThat(views).hasSize(1);
        assertThat(views.getFirst().id()).isEqualTo(row.id());
    }
}
