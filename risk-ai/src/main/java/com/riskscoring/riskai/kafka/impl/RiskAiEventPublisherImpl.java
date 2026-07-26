package com.riskscoring.riskai.kafka.impl;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.ScanCompleted;
import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.riskai.kafka.RiskAiEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskAiEventPublisherImpl implements RiskAiEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishScanCompleted(ScanCompleted event) {
        kafkaTemplate.send(Topics.SCAN_COMPLETED, event.scanId().toString(), event);
        log.info("Published {} scanId={} verdict={}",
                Topics.SCAN_COMPLETED, event.scanId(), event.verdict().riskLevel());
    }

    @Override
    public void publishScanProgress(ScanProgress event) {
        kafkaTemplate.send(Topics.SCAN_PROGRESS, event.scanId().toString(), event);
        log.debug("Published {} scanId={} stage={}", Topics.SCAN_PROGRESS, event.scanId(), event.stage());
    }
}
