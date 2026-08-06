package com.riskscoring.gateway.kafka.impl;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
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
class ScanEventPublisherImplTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ScanEventPublisherImpl publisher;

    @BeforeEach
    void setUp() {
        publisher = new ScanEventPublisherImpl(kafkaTemplate);
    }

    @Test
    void publishScanRequestedSendsToScanRequestedTopicKeyedByScanId() {
        UUID scanId = UUID.randomUUID();
        ScanRequested event = new ScanRequested(scanId, ScanTarget.ADDRESS, "0xtarget", Chain.ETHEREUM,
                Instant.now(), ScanSource.USER, Language.EN, UUID.randomUUID());

        publisher.publishScanRequested(event);

        verify(kafkaTemplate).send(Topics.SCAN_REQUESTED, scanId.toString(), event);
    }
}
