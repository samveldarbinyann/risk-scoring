package com.riskscoring.gateway.kafka;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.gateway.service.ScanProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScanProgressListener {

    private final ScanProgressService scanProgressService;

    @KafkaListener(topics = Topics.SCAN_PROGRESS)
    public void onScanProgress(ScanProgress event) {
        log.debug("Received {} scanId={} stage={}", Topics.SCAN_PROGRESS, event.scanId(), event.stage());
        scanProgressService.applyProgress(event);
    }
}