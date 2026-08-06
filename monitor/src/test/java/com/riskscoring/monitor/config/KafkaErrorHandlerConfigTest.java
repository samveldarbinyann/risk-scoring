package com.riskscoring.monitor.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class KafkaErrorHandlerConfigTest {

    @Mock
    private Consumer<?, ?> consumer;

    @Mock
    private MessageListenerContainer container;

    private final KafkaErrorHandlerConfig config = new KafkaErrorHandlerConfig();

    @Test
    void kafkaErrorHandlerBuildsANonNullDefaultErrorHandler() {
        assertThat(config.kafkaErrorHandler()).isInstanceOf(DefaultErrorHandler.class);
    }

    @Test
    void recovererKeepsRetryingBeforeMaxAttemptsAreExhausted() {
        DefaultErrorHandler errorHandler = config.kafkaErrorHandler();
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("scan.completed", 0, 0L, "key", "value");

        assertThatThrownBy(() -> errorHandler.handleRemaining(
                new RuntimeException("boom"), List.of(record), consumer, container))
                .satisfies(exception -> assertThat(exception.getClass().getSimpleName()).isEqualTo("RecordInRetryException"));
    }

    @Test
    void recovererGivesUpAndReturnsNormallyOnceRetriesAreExhausted() {
        DefaultErrorHandler errorHandler = config.kafkaErrorHandler();
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("scan.completed", 0, 0L, "key", "value");

        // FixedBackOff allows 2 retries: first two calls for the same record keep retrying,
        // the third exhausts the backoff and the recoverer logs and returns normally.
        for (int attempt = 0; attempt < 2; attempt++) {
            assertThatThrownBy(() -> errorHandler.handleRemaining(
                    new RuntimeException("boom"), List.of(record), consumer, container))
                    .satisfies(exception -> assertThat(exception.getClass().getSimpleName()).isEqualTo("RecordInRetryException"));
        }

        assertThatCode(() -> errorHandler.handleRemaining(
                new RuntimeException("boom"), List.of(record), consumer, container))
                .doesNotThrowAnyException();
    }
}
