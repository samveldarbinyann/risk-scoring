package com.riskscoring.chainingest.kafka;

import com.riskscoring.chainingest.service.ChainIngestService;
import com.riskscoring.common.Topics;
import com.riskscoring.common.event.ScanRequested;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScanRequestedListener {

    private final ChainIngestService chainIngestService;

    @KafkaListener(topics = Topics.SCAN_REQUESTED)
    public void onScanRequested(ScanRequested event) {
        log.info("Received {} scanId={} {}={}",
                Topics.SCAN_REQUESTED, event.scanId(), event.targetType(), event.target());
        chainIngestService.ingest(event);
    }
}