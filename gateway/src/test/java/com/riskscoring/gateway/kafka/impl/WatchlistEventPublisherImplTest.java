package com.riskscoring.gateway.kafka.impl;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.WatchlistAddRequested;
import com.riskscoring.common.event.WatchlistRemoveRequested;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WatchlistEventPublisherImplTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private WatchlistEventPublisherImpl publisher;

    @BeforeEach
    void setUp() {
        publisher = new WatchlistEventPublisherImpl(kafkaTemplate);
    }

    @Test
    void publishWatchlistAddRequestedSendsToTopicKeyedByUserChainAddress() {
        UUID userId = UUID.randomUUID();
        WatchlistAddRequested event = new WatchlistAddRequested(
                UUID.randomUUID(), userId, "0xabc", Chain.ETHEREUM, Language.EN, Instant.now());

        publisher.publishWatchlistAddRequested(event);

        String expectedKey = "%s:%s:%s".formatted(userId, Chain.ETHEREUM, "0xabc");
        verify(kafkaTemplate).send(Topics.WATCHLIST_ADD_REQUESTED, expectedKey, event);
    }

    @Test
    void publishWatchlistRemoveRequestedSendsToTopicKeyedByEntryId() {
        UUID entryId = UUID.randomUUID();
        WatchlistRemoveRequested event = new WatchlistRemoveRequested(
                UUID.randomUUID(), UUID.randomUUID(), entryId, Instant.now());

        publisher.publishWatchlistRemoveRequested(event);

        verify(kafkaTemplate).send(Topics.WATCHLIST_REMOVE_REQUESTED, entryId.toString(), event);
    }
}
