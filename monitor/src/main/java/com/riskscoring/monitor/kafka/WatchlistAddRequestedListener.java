package com.riskscoring.monitor.kafka;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.WatchlistAddRequested;
import com.riskscoring.common.model.Chain;
import com.riskscoring.monitor.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchlistAddRequestedListener {

    private final WatchlistService watchlistService;

    @KafkaListener(topics = Topics.WATCHLIST_ADD_REQUESTED)
    public void onWatchlistAddRequested(WatchlistAddRequested event) {
        log.info("Received {} userId={} address={} chain={}",
                Topics.WATCHLIST_ADD_REQUESTED, event.userId(), event.address(), event.chain());
        watchlistService.addEntry(event);
    }
}
