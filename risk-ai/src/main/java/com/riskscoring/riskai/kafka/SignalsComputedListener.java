package com.riskscoring.riskai.kafka;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.SignalsComputed;
import com.riskscoring.riskai.service.RiskAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignalsComputedListener {

    private final RiskAiService riskAiService;

    @KafkaListener(topics = Topics.SIGNALS_COMPUTED)
    public void onSignalsComputed(SignalsComputed event) {
        log.info("Received {} scanId={} address={}", Topics.SIGNALS_COMPUTED, event.scanId(), event.address());
        riskAiService.analyze(event);
    }
}
