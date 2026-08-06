package com.riskscoring.gateway.kafka;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanSource;
import com.riskscoring.gateway.service.ScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScanRequestedListener {

    private final ScanService scanService;

    @KafkaListener(topics = Topics.SCAN_REQUESTED)
    public void onScanRequested(ScanRequested event) {
        if (event.source() != ScanSource.MONITOR) {
            return;
        }

        log.debug("Received {} scanId={} source=MONITOR", Topics.SCAN_REQUESTED, event.scanId());
        scanService.ingestMonitorScan(event);
    }
}
