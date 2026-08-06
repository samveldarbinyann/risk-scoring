package com.riskscoring.monitor.kafka.impl;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.AlertTriggered;
import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.common.model.ScanTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MonitorEventPublisherImplTest {

    private static final Instant NOW = Instant.parse("2024-06-01T00:00:00Z");

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private MonitorEventPublisherImpl publisher;

    @BeforeEach
    void setUp() {
        publisher = new MonitorEventPublisherImpl(kafkaTemplate);
    }

    @Test
    void publishScanRequestedSendsToScanRequestedTopicKeyedByScanId() {
        UUID scanId = UUID.randomUUID();
        ScanRequested event = new ScanRequested(scanId, ScanTarget.ADDRESS, "0xtarget", Chain.ETHEREUM,
                NOW, ScanSource.MONITOR, Language.EN, UUID.randomUUID());

        publisher.publishScanRequested(event);

        verify(kafkaTemplate).send(Topics.SCAN_REQUESTED, scanId.toString(), event);
    }

    @Test
    void publishAlertTriggeredSendsToAlertTriggeredTopicKeyedByAlertId() {
        UUID alertId = UUID.randomUUID();
        AlertTriggered event = new AlertTriggered(alertId, UUID.randomUUID(), UUID.randomUUID(), "0xtarget",
                Chain.ETHEREUM, RiskLevel.LOW, 10, RiskLevel.HIGH, 70, UUID.randomUUID(), NOW);

        publisher.publishAlertTriggered(event);

        verify(kafkaTemplate).send(Topics.ALERT_TRIGGERED, alertId.toString(), event);
    }
}
