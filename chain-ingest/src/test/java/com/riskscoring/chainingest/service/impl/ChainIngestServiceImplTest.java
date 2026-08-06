package com.riskscoring.chainingest.service.impl;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.ChainDataClientKey;
import com.riskscoring.chainingest.exception.UnsupportedChainException;
import com.riskscoring.chainingest.kafka.ChainEventPublisher;
import com.riskscoring.chainingest.service.ChainFactsCacheService;
import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFacts;
import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.TransactionFacts;
import com.riskscoring.common.model.TransactionSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChainIngestServiceImplTest {

    private static final UUID SCAN_ID = UUID.randomUUID();

    @Mock
    private ChainDataClient chainDataClient;

    @Mock
    private ChainFactsCacheService cacheService;

    @Mock
    private ChainEventPublisher chainEventPublisher;

    private ChainIngestServiceImpl service;

    @BeforeEach
    void setUp() {
        Map<ChainDataClientKey, ChainDataClient> clients =
                Map.of(new ChainDataClientKey(ChainFamily.EVM, ScanTarget.ADDRESS), chainDataClient);
        Map<ScanTarget, ChainFactsCacheService> caches = Map.of(ScanTarget.ADDRESS, cacheService);
        service = new ChainIngestServiceImpl(clients, caches, chainEventPublisher);
    }

    private static ScanRequested request(Chain chain, ScanTarget targetType) {
        return new ScanRequested(SCAN_ID, targetType, "0xabc", chain, Instant.now(), ScanSource.USER, Language.EN, UUID.randomUUID());
    }

    @Test
    void unsupportedChainThrowsBeforePublishingAnyProgress() {
        ScanRequested event = request(Chain.BITCOIN, ScanTarget.ADDRESS);

        assertThatThrownBy(() -> service.ingest(event)).isInstanceOf(UnsupportedChainException.class);

        verifyNoInteractions(chainEventPublisher);
    }

    @Test
    void cacheHitSkipsFetchAndStoreAndPublishesFetchedFacts() {
        ScanRequested event = request(Chain.ETHEREUM, ScanTarget.ADDRESS);
        AddressFacts facts = new AddressFacts(
                new AddressSnapshot(5, 1, "100", List.of(), null, null, false, Instant.now()), List.of());
        when(cacheService.findFresh("0xabc", Chain.ETHEREUM)).thenReturn(Optional.of(facts));

        service.ingest(event);

        verify(chainDataClient, never()).fetch(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(cacheService, never()).store(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(chainEventPublisher, times(2)).publishScanProgress(org.mockito.ArgumentMatchers.any());
        ArgumentCaptor<ChainFetched> fetchedCaptor = ArgumentCaptor.forClass(ChainFetched.class);
        verify(chainEventPublisher).publishChainFetched(fetchedCaptor.capture());
        assertThat(fetchedCaptor.getValue().facts()).isSameAs(facts);
        assertThat(fetchedCaptor.getValue().scanId()).isEqualTo(SCAN_ID);
    }

    @Test
    void cacheMissFetchesAndStoresThenPublishes() {
        ScanRequested event = request(Chain.ETHEREUM, ScanTarget.ADDRESS);
        AddressFacts facts = new AddressFacts(
                new AddressSnapshot(5, 1, "100", List.of(), null, null, false, Instant.now()), List.of());
        when(cacheService.findFresh("0xabc", Chain.ETHEREUM)).thenReturn(Optional.empty());
        when(chainDataClient.fetch("0xabc", Chain.ETHEREUM)).thenReturn(facts);

        service.ingest(event);

        verify(chainDataClient).fetch("0xabc", Chain.ETHEREUM);
        verify(cacheService).store("0xabc", Chain.ETHEREUM, facts);
        verify(chainEventPublisher).publishChainFetched(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void progressMessageForAddressFactsUsesAddressKeyAndArgs() {
        ScanRequested event = request(Chain.ETHEREUM, ScanTarget.ADDRESS);
        AddressFacts facts = new AddressFacts(
                new AddressSnapshot(5, 1, "100", List.of(), null, null, false, Instant.now()),
                List.of(new com.riskscoring.common.model.Counterparty("0xcp", com.riskscoring.common.model.TransferDirection.OUT, 1, "1", 1)));
        when(cacheService.findFresh("0xabc", Chain.ETHEREUM)).thenReturn(Optional.of(facts));

        service.ingest(event);

        ArgumentCaptor<ScanProgress> captor = ArgumentCaptor.forClass(ScanProgress.class);
        verify(chainEventPublisher, times(2)).publishScanProgress(captor.capture());
        ScanProgress done = captor.getAllValues().get(1);
        assertThat(done.messageKey()).isEqualTo("console.message.fetchDoneAddress");
        assertThat(done.messageArgs()).isEqualTo(List.of(5L, 1));
    }

    @Test
    void progressMessageForTransactionFactsUsesTransactionKeyAndArgs() {
        Map<ChainDataClientKey, ChainDataClient> clients =
                Map.of(new ChainDataClientKey(ChainFamily.EVM, ScanTarget.TRANSACTION), chainDataClient);
        Map<ScanTarget, ChainFactsCacheService> caches = Map.of(ScanTarget.TRANSACTION, cacheService);
        service = new ChainIngestServiceImpl(clients, caches, chainEventPublisher);

        ScanRequested event = request(Chain.ETHEREUM, ScanTarget.TRANSACTION);
        TransactionFacts facts = new TransactionFacts(new TransactionSnapshot(
                "0xabc", "from", "to", "0", true, Instant.now(), List.of(), 3, 0, List.of(), Instant.now()));
        when(cacheService.findFresh("0xabc", Chain.ETHEREUM)).thenReturn(Optional.of(facts));

        service.ingest(event);

        ArgumentCaptor<ScanProgress> captor = ArgumentCaptor.forClass(ScanProgress.class);
        verify(chainEventPublisher, times(2)).publishScanProgress(captor.capture());
        ScanProgress done = captor.getAllValues().get(1);
        assertThat(done.messageKey()).isEqualTo("console.message.fetchDoneTransaction");
        assertThat(done.messageArgs()).isEqualTo(List.of(0, 3));
    }

    @Test
    void chainFetchedEventEchoesOriginalRequestFields() {
        ScanRequested event = request(Chain.ETHEREUM, ScanTarget.ADDRESS);
        AddressFacts facts = new AddressFacts(
                new AddressSnapshot(5, 1, "100", List.of(), null, null, false, Instant.now()), List.of());
        when(cacheService.findFresh("0xabc", Chain.ETHEREUM)).thenReturn(Optional.of(facts));

        service.ingest(event);

        ArgumentCaptor<ChainFetched> captor = ArgumentCaptor.forClass(ChainFetched.class);
        verify(chainEventPublisher).publishChainFetched(captor.capture());
        ChainFetched published = captor.getValue();
        assertThat(published.scanId()).isEqualTo(event.scanId());
        assertThat(published.targetType()).isEqualTo(event.targetType());
        assertThat(published.target()).isEqualTo(event.target());
        assertThat(published.chain()).isEqualTo(event.chain());
        assertThat(published.language()).isEqualTo(event.language());
    }
}
