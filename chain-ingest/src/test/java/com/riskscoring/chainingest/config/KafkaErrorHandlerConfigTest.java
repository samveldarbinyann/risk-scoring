package com.riskscoring.chainingest.config;

import com.riskscoring.chainingest.exception.ChainDataRejectedException;
import com.riskscoring.chainingest.exception.UnsupportedChainException;
import com.riskscoring.chainingest.kafka.ChainEventPublisher;
import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaErrorHandlerConfigTest {

    private static final UUID SCAN_ID = UUID.randomUUID();

    @Mock
    private ChainEventPublisher eventPublisher;

    @Mock
    private Consumer<?, ?> consumer;

    @Mock
    private MessageListenerContainer container;

    private final KafkaErrorHandlerConfig config = new KafkaErrorHandlerConfig();

    @Test
    void kafkaErrorHandlerBuildsANonNullDefaultErrorHandler() {
        assertThat(config.kafkaErrorHandler(eventPublisher)).isInstanceOf(DefaultErrorHandler.class);
    }

    @Test
    void nonRetryableFailureWithUserFacingCauseUsesItsMessageKeyAndArgs() {
        DefaultErrorHandler errorHandler = config.kafkaErrorHandler(eventPublisher);
        ScanRequested event = scanRequested();
        UnsupportedChainException failure = new UnsupportedChainException(Chain.SUI, ScanTarget.ADDRESS);
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("scan.requested", 0, 0L, "key", event);

        errorHandler.handleRemaining(failure, List.of(record), consumer, container);

        ArgumentCaptor<ScanProgress> captor = ArgumentCaptor.forClass(ScanProgress.class);
        verify(eventPublisher).publishScanProgress(captor.capture());
        ScanProgress progress = captor.getValue();
        assertThat(progress.scanId()).isEqualTo(SCAN_ID);
        assertThat(progress.stage()).isEqualTo(ScanStage.FAILED);
        assertThat(progress.messageKey()).isEqualTo(failure.progressMessageKey());
        assertThat(progress.messageArgs()).isEqualTo(failure.progressMessageArgs());
    }

    @Test
    void nonRetryableFailureWithoutUserFacingCauseFallsBackToGenericKeyWithEmptyArgs() {
        DefaultErrorHandler errorHandler = config.kafkaErrorHandler(eventPublisher);
        ScanRequested event = scanRequested();
        ChainDataRejectedException failure = new ChainDataRejectedException("provider rejected the request");
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("scan.requested", 0, 0L, "key", event);

        errorHandler.handleRemaining(failure, List.of(record), consumer, container);

        ArgumentCaptor<ScanProgress> captor = ArgumentCaptor.forClass(ScanProgress.class);
        verify(eventPublisher).publishScanProgress(captor.capture());
        ScanProgress progress = captor.getValue();
        assertThat(progress.messageKey()).isEqualTo("console.message.chainFetchFailed");
        assertThat(progress.messageArgs()).isEmpty();
    }

    @Test
    void doesNotPublishProgressWhenRecordValueIsNotScanRequested() {
        DefaultErrorHandler errorHandler = config.kafkaErrorHandler(eventPublisher);
        ChainDataRejectedException failure = new ChainDataRejectedException("provider rejected the request");
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("scan.requested", 0, 0L, "key", "not-a-scan-requested");

        errorHandler.handleRemaining(failure, List.of(record), consumer, container);

        org.mockito.Mockito.verifyNoInteractions(eventPublisher);
    }

    private static ScanRequested scanRequested() {
        return new ScanRequested(SCAN_ID, ScanTarget.ADDRESS, "target", Chain.ETHEREUM,
                Instant.now(), ScanSource.USER, Language.EN, UUID.randomUUID());
    }
}
