package com.riskscoring.enrichment.service.impl;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.LabelCategory;
import com.riskscoring.enrichment.client.OfacSdnClient;
import com.riskscoring.enrichment.client.dto.OfacDigitalCurrencyAddress;
import com.riskscoring.enrichment.entity.Label;
import com.riskscoring.enrichment.mapper.OfacChainMapping;
import com.riskscoring.enrichment.repository.LabelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabelSyncServiceImplTest {

    private static final String SOURCE = "OFAC SDN";

    @Mock
    private OfacSdnClient ofacSdnClient;
    @Mock
    private OfacChainMapping chainMapping;
    @Mock
    private LabelRepository labelRepository;

    private LabelSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LabelSyncServiceImpl(ofacSdnClient, chainMapping, labelRepository);
    }

    @Test
    void addsNewLabelsUpdatesChangedNamesAndRemovesDelistedOnes() {
        OfacDigitalCurrencyAddress newAddress = new OfacDigitalCurrencyAddress("XBT", "1NewAddress", "New Entity");
        OfacDigitalCurrencyAddress renamedAddress =
                new OfacDigitalCurrencyAddress("XBT", "1RenamedAddress", "Renamed Entity");
        OfacDigitalCurrencyAddress unchangedAddress =
                new OfacDigitalCurrencyAddress("XBT", "1UnchangedAddress", "Same Entity");

        when(ofacSdnClient.fetchDigitalCurrencyAddresses())
                .thenReturn(List.of(newAddress, renamedAddress, unchangedAddress));
        when(chainMapping.chainsFor("XBT")).thenReturn(List.of(Chain.BITCOIN));

        Label existingRenamed = existingLabel(Chain.BITCOIN, "1RenamedAddress", "Old Entity Name");
        Label existingUnchanged = existingLabel(Chain.BITCOIN, "1UnchangedAddress", "Same Entity");
        Label existingDelisted = existingLabel(Chain.BITCOIN, "1DelistedAddress", "Delisted Entity");

        when(labelRepository.findByCategoryAndSource(LabelCategory.SANCTION, SOURCE))
                .thenReturn(List.of(existingRenamed, existingUnchanged, existingDelisted));

        service.syncOfacSanctions();

        ArgumentCaptor<List<Label>> savedCaptor = ArgumentCaptor.forClass(List.class);
        verify(labelRepository).saveAll(savedCaptor.capture());
        List<Label> saved = savedCaptor.getValue();

        assertThat(saved).hasSize(2);
        assertThat(saved).anySatisfy(label -> {
            assertThat(label.getAddress()).isEqualTo("1NewAddress");
            assertThat(label.getName()).isEqualTo("New Entity");
            assertThat(label.getChain()).isEqualTo(Chain.BITCOIN);
            assertThat(label.getCategory()).isEqualTo(LabelCategory.SANCTION);
            assertThat(label.getSource()).isEqualTo(SOURCE);
        });
        assertThat(saved).anySatisfy(label -> {
            assertThat(label.getAddress()).isEqualTo("1RenamedAddress");
            assertThat(label.getName()).isEqualTo("Renamed Entity");
        });

        ArgumentCaptor<List<Label>> deletedCaptor = ArgumentCaptor.forClass(List.class);
        verify(labelRepository).deleteAll(deletedCaptor.capture());
        assertThat(deletedCaptor.getValue()).containsExactly(existingDelisted);
    }

    @Test
    void skipsSyncEntirelyWhenFetchFails() {
        when(ofacSdnClient.fetchDigitalCurrencyAddresses()).thenThrow(new RuntimeException("network error"));

        service.syncOfacSanctions();

        verifyNoInteractions(labelRepository);
    }

    private static Label existingLabel(Chain chain, String address, String name) {
        return Label.builder()
                .id(UUID.randomUUID())
                .chain(chain)
                .address(address)
                .category(LabelCategory.SANCTION)
                .name(name)
                .source(SOURCE)
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
