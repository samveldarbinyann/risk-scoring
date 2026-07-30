package com.riskscoring.monitor.repository;

import com.riskscoring.monitor.entity.WatchlistEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchlistEntryRepository extends JpaRepository<WatchlistEntry, UUID> {

    Optional<WatchlistEntry> findByUserIdAndChainIdAndAddress(UUID userId, int chainId, String address);

    Optional<WatchlistEntry> findByIdAndUserId(UUID id, UUID userId);

    Optional<WatchlistEntry> findByPendingScanId(UUID pendingScanId);

    @Query("""
            SELECT w FROM WatchlistEntry w
            WHERE w.active = true
              AND (w.pendingScanId IS NULL OR w.pendingRequestedAt < :staleThreshold)
              AND (w.lastCheckedAt IS NULL OR w.lastCheckedAt < :dueThreshold)
            ORDER BY w.lastCheckedAt ASC NULLS FIRST
            """)
    List<WatchlistEntry> findDueForRecheck(@Param("dueThreshold") Instant dueThreshold,
                                            @Param("staleThreshold") Instant staleThreshold,
                                            Pageable pageable);
}
