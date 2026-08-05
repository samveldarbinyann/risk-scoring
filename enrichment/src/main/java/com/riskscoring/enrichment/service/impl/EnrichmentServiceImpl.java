package com.riskscoring.enrichment.service.impl;

import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.event.SignalsComputed;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.enrichment.entity.Label;
import com.riskscoring.enrichment.kafka.EnrichmentEventPublisher;
import com.riskscoring.enrichment.mapper.EvidenceMapper;
import com.riskscoring.enrichment.repository.EvidenceRecordRepository;
import com.riskscoring.enrichment.repository.LabelRepository;
import com.riskscoring.enrichment.service.EnrichmentService;
import com.riskscoring.enrichment.service.RiskSignalCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrichmentServiceImpl implements EnrichmentService {

    private static final String PROGRESS_MESSAGE_KEY = "console.message.enriching";

    private final LabelRepository labelRepository;
    private final EvidenceRecordRepository evidenceRecordRepository;
    private final RiskSignalCalculator riskSignalCalculator;
    private final EvidenceMapper evidenceMapper;
    private final EnrichmentEventPublisher eventPublisher;

    @Override
    @Transactional
    public void enrich(ChainFetched event) {
        eventPublisher.publishScanProgress(
                new ScanProgress(event.scanId(), ScanStage.ENRICHING, PROGRESS_MESSAGE_KEY, List.of(), event.language(), Instant.now()));

        Map<String, Label> labels = findLabels(event);
        EvidenceBundle evidence = riskSignalCalculator.calculate(event, labels);

        evidenceRecordRepository.save(evidenceMapper.toRecord(event, evidence, Instant.now()));

        log.info("Signals for scanId={} targetType={} labels={}",
                event.scanId(), event.targetType(), labels.size());

        eventPublisher.publishSignalsComputed(new SignalsComputed(
                event.scanId(),
                event.targetType(),
                event.target(),
                event.chain(),
                evidence,
                event.language(),
                Instant.now()
        ));
    }

    private Map<String, Label> findLabels(ChainFetched event) {
        return labelRepository.findByChainAndAddressIn(event.chain(), riskSignalCalculator.addressesToLabel(event)).stream()
                .collect(Collectors.toMap(Label::getAddress, Function.identity()));
    }
}
