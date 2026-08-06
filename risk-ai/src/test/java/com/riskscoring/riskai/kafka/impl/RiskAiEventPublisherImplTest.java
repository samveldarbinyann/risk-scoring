package com.riskscoring.riskai.kafka.impl;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.ScanCompleted;
import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.Verdict;
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
class RiskAiEventPublisherImplTest {

    private static final Instant NOW = Instant.parse("2024-06-01T00:00:00Z");

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private RiskAiEventPublisherImpl publisher;

    @BeforeEach
    void setUp() {
        publisher = new RiskAiEventPublisherImpl(kafkaTemplate);
    }

    @Test
    void publishScanCompletedSendsToScanCompletedTopicKeyedByScanId() {
        UUID scanId = UUID.randomUUID();
        Verdict verdict = new Verdict(RiskLevel.LOW, 10, "clean", List.of(), List.of());
        ScanCompleted event = new ScanCompleted(
                scanId, ScanTarget.ADDRESS, "0xtarget", Chain.ETHEREUM, verdict, "deepseek-chat", NOW);

        publisher.publishScanCompleted(event);

        verify(kafkaTemplate).send(Topics.SCAN_COMPLETED, scanId.toString(), event);
    }

    @Test
    void publishScanProgressSendsToScanProgressTopicKeyedByScanId() {
        UUID scanId = UUID.randomUUID();
        ScanProgress event = new ScanProgress(scanId, ScanStage.ANALYZING, "console.message.analyzing",
                List.of(), Language.EN, NOW);

        publisher.publishScanProgress(event);

        verify(kafkaTemplate).send(Topics.SCAN_PROGRESS, scanId.toString(), event);
    }
}
