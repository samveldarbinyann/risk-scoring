package com.riskscoring.gateway.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchlistRepository {

    List<WatchlistEntryRow> findAllByUserId(UUID userId);

    Optional<WatchlistEntryRow> findByIdAndUserId(UUID id, UUID userId);
}
