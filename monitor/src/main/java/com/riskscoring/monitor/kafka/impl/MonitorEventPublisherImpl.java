package com.riskscoring.monitor.kafka.impl;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.AlertTriggered;
import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.model.Chain;
import com.riskscoring.monitor.kafka.MonitorEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitorEventPublisherImpl implements MonitorEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishScanRequested(ScanRequested event) {
        kafkaTemplate.send(Topics.SCAN_REQUESTED, event.scanId().toString(), event);
        log.info("Published {} scanId={} address={} chain={} source={}",
                Topics.SCAN_REQUESTED, event.scanId(), event.target(), event.chain(), event.source());
    }

    @Override
    public void publishAlertTriggered(AlertTriggered event) {
        kafkaTemplate.send(Topics.ALERT_TRIGGERED, event.alertId().toString(), event);
        log.info("Published {} alertId={} watchlistEntryId={} {} -> {}",
                Topics.ALERT_TRIGGERED, event.alertId(), event.watchlistEntryId(),
                event.previousRiskLevel(), event.newRiskLevel());
    }
}
