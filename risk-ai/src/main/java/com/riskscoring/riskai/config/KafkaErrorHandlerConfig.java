package com.riskscoring.riskai.config;

import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.event.SignalsComputed;
import com.riskscoring.riskai.kafka.RiskAiEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Instant;

@Configuration
@Slf4j
public class KafkaErrorHandlerConfig {

    private static final long RETRY_INTERVAL_MS = 5_000L;
    private static final long MAX_RETRIES = 2L;
    private static final String FAILURE_MESSAGE = "AI analysis failed";

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(RiskAiEventPublisher eventPublisher) {
        return new DefaultErrorHandler(
                (record, exception) -> {
                    log.error("Giving up on {} after {} retries", record.topic(), MAX_RETRIES, exception);

                    if (record.value() instanceof SignalsComputed event) {
                        eventPublisher.publishScanProgress(new ScanProgress(
                                event.scanId(), ScanStage.FAILED, FAILURE_MESSAGE, Instant.now()));
                    }
                },
                new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRIES));
    }
}
