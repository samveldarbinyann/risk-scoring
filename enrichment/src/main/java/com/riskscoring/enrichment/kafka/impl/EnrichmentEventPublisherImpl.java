package com.riskscoring.enrichment.kafka.impl;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.SignalsComputed;
import com.riskscoring.enrichment.kafka.EnrichmentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrichmentEventPublisherImpl implements EnrichmentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishSignalsComputed(SignalsComputed event) {
        kafkaTemplate.send(Topics.SIGNALS_COMPUTED, event.scanId().toString(), event);
        log.info("Published {} scanId={}", Topics.SIGNALS_COMPUTED, event.scanId());
    }

    @Override
    public void publishScanProgress(ScanProgress event) {
        kafkaTemplate.send(Topics.SCAN_PROGRESS, event.scanId().toString(), event);
        log.debug("Published {} scanId={} stage={}", Topics.SCAN_PROGRESS, event.scanId(), event.stage());
    }
}