package com.riskscoring.gateway.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class WatchlistEntryNotFoundException extends ApiException {

    public WatchlistEntryNotFoundException(UUID entryId) {
        super(HttpStatus.NOT_FOUND, "WATCHLIST_ENTRY_NOT_FOUND", "error.watchlistEntryNotFound", entryId);
    }
}
