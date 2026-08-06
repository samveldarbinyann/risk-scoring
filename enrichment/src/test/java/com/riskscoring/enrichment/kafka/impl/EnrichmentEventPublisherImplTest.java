package com.riskscoring.enrichment.kafka.impl;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.event.SignalsComputed;
import com.riskscoring.common.model.AddressEvidence;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Heuristics;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnrichmentEventPublisherImplTest {

    private static final Instant NOW = Instant.parse("2024-06-01T00:00:00Z");

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private EnrichmentEventPublisherImpl publisher;

    @BeforeEach
    void setUp() {
        publisher = new EnrichmentEventPublisherImpl(kafkaTemplate);
    }

    @Test
    void publishSignalsComputedSendsToSignalsComputedTopicKeyedByScanId() {
        UUID scanId = UUID.randomUUID();
        SignalsComputed event = new SignalsComputed(scanId, ScanTarget.ADDRESS, "0xtarget", Chain.ETHEREUM,
                new AddressEvidence("0xtarget", Chain.ETHEREUM, NOW, null, 0, 0, false, "0", List.of(), 0,
                        List.of(), null, new Heuristics(null, null, false, 0, 0)),
                Language.EN, NOW);

        publisher.publishSignalsComputed(event);

        verify(kafkaTemplate).send(Topics.SIGNALS_COMPUTED, scanId.toString(), event);
    }

    @Test
    void publishScanProgressSendsToScanProgressTopicKeyedByScanId() {
        UUID scanId = UUID.randomUUID();
        ScanProgress event = new ScanProgress(scanId, ScanStage.ENRICHING, "console.message.enriching",
                List.of(), Language.EN, NOW);

        publisher.publishScanProgress(event);

        verify(kafkaTemplate).send(Topics.SCAN_PROGRESS, scanId.toString(), event);
    }
}
