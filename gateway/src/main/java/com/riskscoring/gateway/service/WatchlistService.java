package com.riskscoring.gateway.service;

import com.riskscoring.gateway.dto.WatchlistCreateRequest;
import com.riskscoring.gateway.dto.WatchlistEntryView;

import java.util.List;
import java.util.UUID;

public interface WatchlistService {

    void addToWatchlist(UUID userId, WatchlistCreateRequest request);

    void removeFromWatchlist(UUID userId, UUID entryId);

    List<WatchlistEntryView> listWatchlist(UUID userId);
}
