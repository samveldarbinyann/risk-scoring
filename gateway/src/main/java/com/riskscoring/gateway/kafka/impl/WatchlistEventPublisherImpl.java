package com.riskscoring.gateway.kafka.impl;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.WatchlistAddRequested;
import com.riskscoring.common.event.WatchlistRemoveRequested;
import com.riskscoring.gateway.kafka.WatchlistEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchlistEventPublisherImpl implements WatchlistEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishWatchlistAddRequested(WatchlistAddRequested event) {
        String key = "%s:%s:%s".formatted(event.userId(), event.chain(), event.address());
        kafkaTemplate.send(Topics.WATCHLIST_ADD_REQUESTED, key, event);
        log.info("Published {} userId={} address={} chain={}",
                Topics.WATCHLIST_ADD_REQUESTED, event.userId(), event.address(), event.chain());
    }

    @Override
    public void publishWatchlistRemoveRequested(WatchlistRemoveRequested event) {
        kafkaTemplate.send(Topics.WATCHLIST_REMOVE_REQUESTED, event.entryId().toString(), event);
        log.info("Published {} userId={} entryId={}",
                Topics.WATCHLIST_REMOVE_REQUESTED, event.userId(), event.entryId());
    }
}
