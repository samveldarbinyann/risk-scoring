package com.riskscoring.gateway.kafka;

import com.riskscoring.common.event.WatchlistAddRequested;
import com.riskscoring.common.event.WatchlistRemoveRequested;

public interface WatchlistEventPublisher {

    void publishWatchlistAddRequested(WatchlistAddRequested event);

    void publishWatchlistRemoveRequested(WatchlistRemoveRequested event);
}
