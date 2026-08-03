package com.riskscoring.chainingest.config;

import com.riskscoring.chainingest.exception.ChainDataNotFoundException;
import com.riskscoring.chainingest.exception.ChainDataRejectedException;
import com.riskscoring.chainingest.exception.UnsupportedChainException;
import com.riskscoring.chainingest.exception.UserFacingChainFailure;
import com.riskscoring.chainingest.kafka.ChainEventPublisher;
import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Instant;

@Configuration
@Slf4j
public class KafkaErrorHandlerConfig {

    private static final long RETRY_INTERVAL_MS = 5_000L;
    private static final long MAX_RETRIES = 2L;
    private static final String FAILURE_MESSAGE = "Chain data fetch failed";

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(ChainEventPublisher eventPublisher) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> {
                    log.error("Giving up on {}-{} offset {}", record.topic(), record.partition(),
                            record.offset(), exception);

                    if (record.value() instanceof ScanRequested event) {
                        eventPublisher.publishScanProgress(new ScanProgress(
                                event.scanId(), ScanStage.FAILED, failureMessage(exception), Instant.now()));
                    }
                },
                new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRIES));

        errorHandler.addNotRetryableExceptions(
                UnsupportedChainException.class,
                ChainDataRejectedException.class,
                ChainDataNotFoundException.class);

        return errorHandler;
    }

    private static String failureMessage(Exception exception) {
        return NestedExceptionUtils.getMostSpecificCause(exception) instanceof UserFacingChainFailure failure
                ? failure.progressMessage()
                : FAILURE_MESSAGE;
    }
}
