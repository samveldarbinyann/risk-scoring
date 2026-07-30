package com.riskscoring.monitor.service;

import com.riskscoring.common.event.WatchlistAddRequested;
import com.riskscoring.common.event.WatchlistRemoveRequested;

public interface WatchlistService {

    void addEntry(WatchlistAddRequested event);

    void removeEntry(WatchlistRemoveRequested event);
}
