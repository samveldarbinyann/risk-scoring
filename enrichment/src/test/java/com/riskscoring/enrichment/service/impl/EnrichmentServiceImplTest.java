package com.riskscoring.enrichment.service.impl;

import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.event.SignalsComputed;
import com.riskscoring.common.model.AddressEvidence;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Heuristics;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.LabelCategory;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.enrichment.entity.EvidenceRecord;
import com.riskscoring.enrichment.entity.Label;
import com.riskscoring.enrichment.kafka.EnrichmentEventPublisher;
import com.riskscoring.enrichment.mapper.EvidenceMapper;
import com.riskscoring.enrichment.repository.EvidenceRecordRepository;
import com.riskscoring.enrichment.repository.LabelRepository;
import com.riskscoring.enrichment.service.RiskSignalCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceImplTest {

    private static final Chain CHAIN = Chain.ETHEREUM;
    private static final String TARGET = "0xtarget";
    private static final Instant NOW = Instant.parse("2024-06-01T00:00:00Z");

    @Mock
    private LabelRepository labelRepository;
    @Mock
    private EvidenceRecordRepository evidenceRecordRepository;
    @Mock
    private RiskSignalCalculator riskSignalCalculator;
    @Mock
    private EvidenceMapper evidenceMapper;
    @Mock
    private EnrichmentEventPublisher eventPublisher;

    private EnrichmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EnrichmentServiceImpl(
                labelRepository, evidenceRecordRepository, riskSignalCalculator, evidenceMapper, eventPublisher);
    }

    @Test
    void enrichCallsCollaboratorsInPipelineOrder() {
        ChainFetched event = event();
        AddressEvidence evidence = evidence();
        EvidenceRecord record = record();
        when(riskSignalCalculator.addressesToLabel(event)).thenReturn(Set.of(TARGET));
        when(labelRepository.findByChainAndAddressIn(CHAIN, Set.of(TARGET))).thenReturn(List.of());
        when(riskSignalCalculator.calculate(event, Map.of())).thenReturn(evidence);
        when(evidenceMapper.toRecord(eq(event), eq(evidence), any(Instant.class))).thenReturn(record);

        service.enrich(event);

        InOrder order = inOrder(eventPublisher, labelRepository, riskSignalCalculator, evidenceRecordRepository);
        order.verify(eventPublisher).publishScanProgress(any(ScanProgress.class));
        order.verify(labelRepository).findByChainAndAddressIn(CHAIN, Set.of(TARGET));
        order.verify(riskSignalCalculator).calculate(event, Map.of());
        order.verify(evidenceRecordRepository).save(record);
        order.verify(eventPublisher).publishSignalsComputed(any(SignalsComputed.class));
    }

    @Test
    void enrichPublishesScanProgressWithEnrichingStage() {
        ChainFetched event = event();
        when(riskSignalCalculator.addressesToLabel(event)).thenReturn(Set.of());
        when(labelRepository.findByChainAndAddressIn(CHAIN, Set.of())).thenReturn(List.of());
        when(riskSignalCalculator.calculate(event, Map.of())).thenReturn(evidence());
        when(evidenceMapper.toRecord(any(), any(), any())).thenReturn(record());

        service.enrich(event);

        ArgumentCaptor<ScanProgress> captor = ArgumentCaptor.forClass(ScanProgress.class);
        verify(eventPublisher).publishScanProgress(captor.capture());
        ScanProgress progress = captor.getValue();
        assertThat(progress.scanId()).isEqualTo(event.scanId());
        assertThat(progress.stage()).isEqualTo(ScanStage.ENRICHING);
        assertThat(progress.messageKey()).isEqualTo("console.message.enriching");
        assertThat(progress.language()).isEqualTo(Language.EN);
    }

    @Test
    void findLabelsBuildsMapKeyedByAddressFromRepositoryResult() {
        ChainFetched event = event();
        Label labelA = label("0xa");
        Label labelB = label("0xb");
        when(riskSignalCalculator.addressesToLabel(event)).thenReturn(Set.of("0xa", "0xb"));
        when(labelRepository.findByChainAndAddressIn(CHAIN, Set.of("0xa", "0xb"))).thenReturn(List.of(labelA, labelB));
        when(riskSignalCalculator.calculate(any(), any())).thenReturn(evidence());
        when(evidenceMapper.toRecord(any(), any(), any())).thenReturn(record());

        service.enrich(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Label>> captor = ArgumentCaptor.forClass(Map.class);
        verify(riskSignalCalculator).calculate(eq(event), captor.capture());
        assertThat(captor.getValue()).containsEntry("0xa", labelA).containsEntry("0xb", labelB);
    }

    @Test
    void enrichPublishesSignalsComputedWithEventAndEvidenceFields() {
        ChainFetched event = event();
        AddressEvidence evidence = evidence();
        when(riskSignalCalculator.addressesToLabel(event)).thenReturn(Set.of());
        when(labelRepository.findByChainAndAddressIn(CHAIN, Set.of())).thenReturn(List.of());
        when(riskSignalCalculator.calculate(event, Map.of())).thenReturn(evidence);
        when(evidenceMapper.toRecord(any(), any(), any())).thenReturn(record());

        service.enrich(event);

        ArgumentCaptor<SignalsComputed> captor = ArgumentCaptor.forClass(SignalsComputed.class);
        verify(eventPublisher).publishSignalsComputed(captor.capture());
        SignalsComputed published = captor.getValue();
        assertThat(published.scanId()).isEqualTo(event.scanId());
        assertThat(published.targetType()).isEqualTo(event.targetType());
        assertThat(published.target()).isEqualTo(event.target());
        assertThat(published.chain()).isEqualTo(event.chain());
        assertThat(published.evidence()).isEqualTo(evidence);
        assertThat(published.language()).isEqualTo(event.language());
    }

    private static ChainFetched event() {
        AddressFacts facts = new AddressFacts(
                new AddressSnapshot(1, 1, "0", List.of(), null, NOW, false, NOW), List.of());
        return new ChainFetched(UUID.randomUUID(), ScanTarget.ADDRESS, TARGET, CHAIN, facts, Language.EN, NOW);
    }

    private static AddressEvidence evidence() {
        return new AddressEvidence(TARGET, CHAIN, NOW, null, 0, 0, false, "0", List.of(), 0, List.of(), null,
                new Heuristics(null, null, false, 0, 0));
    }

    private static EvidenceRecord record() {
        return EvidenceRecord.builder()
                .id(UUID.randomUUID())
                .scanId(UUID.randomUUID())
                .targetType(ScanTarget.ADDRESS)
                .target(TARGET)
                .chain(CHAIN)
                .payload("{}")
                .createdAt(NOW)
                .build();
    }

    private static Label label(String address) {
        return Label.builder()
                .id(UUID.randomUUID())
                .chain(CHAIN)
                .address(address)
                .category(LabelCategory.SANCTION)
                .name("Test Label")
                .source("Test Source")
                .updatedAt(NOW)
                .build();
    }
}
