package com.riskscoring.monitor.kafka;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.ScanCompleted;
import com.riskscoring.monitor.service.RecheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScanCompletedListener {

    private final RecheckService recheckService;

    @KafkaListener(topics = Topics.SCAN_COMPLETED)
    public void onScanCompleted(ScanCompleted event) {
        log.debug("Received {} scanId={} {}={}",
                Topics.SCAN_COMPLETED, event.scanId(), event.targetType(), event.target());
        recheckService.handleScanCompleted(event);
    }
}
