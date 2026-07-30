package com.riskscoring.gateway.repository;

import java.util.List;
import java.util.UUID;

public interface WatchlistRepository {

    List<WatchlistEntryRow> findAllByUserId(UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}