package com.riskscoring.gateway.kafka.impl;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.gateway.kafka.ScanEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScanEventPublisherImpl implements ScanEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishScanRequested(ScanRequested event) {
        kafkaTemplate.send(Topics.SCAN_REQUESTED, event.scanId().toString(), event);
        log.info("Published {} scanId={} {}={} chain={}",
                Topics.SCAN_REQUESTED, event.scanId(), event.targetType(), event.target(), event.chain());
    }
}