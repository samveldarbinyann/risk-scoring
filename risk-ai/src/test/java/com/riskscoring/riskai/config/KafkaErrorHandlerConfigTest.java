package com.riskscoring.riskai.config;

import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.event.SignalsComputed;
import com.riskscoring.common.model.AddressEvidence;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Heuristics;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.riskai.exception.InvalidVerdictException;
import com.riskscoring.riskai.kafka.RiskAiEventPublisher;
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
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class KafkaErrorHandlerConfigTest {

    private static final UUID SCAN_ID = UUID.randomUUID();

    @Mock
    private RiskAiEventPublisher eventPublisher;

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
    void recovererPublishesFailedProgressWithFailureMessageKeyWhenRecordValueIsSignalsComputed() {
        DefaultErrorHandler errorHandler = config.kafkaErrorHandler(eventPublisher);
        SignalsComputed event = signalsComputed();
        InvalidVerdictException failure = new InvalidVerdictException("LLM failed to produce a valid verdict");
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("signals.computed", 0, 0L, "key", event);

        errorHandler.handleRemaining(failure, List.of(record), consumer, container);

        ArgumentCaptor<ScanProgress> captor = ArgumentCaptor.forClass(ScanProgress.class);
        verify(eventPublisher).publishScanProgress(captor.capture());
        ScanProgress progress = captor.getValue();
        assertThat(progress.scanId()).isEqualTo(SCAN_ID);
        assertThat(progress.stage()).isEqualTo(ScanStage.FAILED);
        assertThat(progress.messageKey()).isEqualTo("console.message.aiAnalysisFailed");
        assertThat(progress.messageArgs()).isEmpty();
        assertThat(progress.language()).isEqualTo(event.language());
    }

    @Test
    void recovererDoesNotPublishWhenRecordValueIsNotSignalsComputed() {
        DefaultErrorHandler errorHandler = config.kafkaErrorHandler(eventPublisher);
        InvalidVerdictException failure = new InvalidVerdictException("invalid");
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("signals.computed", 0, 0L, "key", "not-signals-computed");

        errorHandler.handleRemaining(failure, List.of(record), consumer, container);

        verifyNoInteractions(eventPublisher);
    }

    private static SignalsComputed signalsComputed() {
        AddressEvidence evidence = new AddressEvidence("0xtarget", Chain.ETHEREUM, Instant.now(), null, 0, 0,
                false, "0", List.of(), 0, List.of(), null, new Heuristics(null, null, false, 0, 0));
        return new SignalsComputed(SCAN_ID, ScanTarget.ADDRESS, "0xtarget", Chain.ETHEREUM, evidence, Language.EN, Instant.now());
    }
}
