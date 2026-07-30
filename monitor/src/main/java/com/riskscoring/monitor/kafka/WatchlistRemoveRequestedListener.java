package com.riskscoring.monitor.kafka;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.WatchlistRemoveRequested;
import com.riskscoring.monitor.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchlistRemoveRequestedListener {

    private final WatchlistService watchlistService;

    @KafkaListener(topics = Topics.WATCHLIST_REMOVE_REQUESTED)
    public void onWatchlistRemoveRequested(WatchlistRemoveRequested event) {
        log.info("Received {} userId={} entryId={}",
                Topics.WATCHLIST_REMOVE_REQUESTED, event.userId(), event.entryId());
        watchlistService.removeEntry(event);
    }
}
