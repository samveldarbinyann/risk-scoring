package com.riskscoring.gateway.repository;

import com.riskscoring.common.event.ScanSource;
import com.riskscoring.gateway.entity.ScanGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ScanGroupRepository extends JpaRepository<ScanGroup, UUID> {

    List<ScanGroup> findTop5ByUserIdOrderByRequestedAtDesc(UUID userId);

    @Query("""
            SELECT g FROM ScanGroup g
            WHERE g.userId = :userId
              AND (:source IS NULL OR EXISTS (
                  SELECT 1 FROM Scan s WHERE s.groupId = g.id AND s.source = :source))
            ORDER BY g.requestedAt DESC
            """)
    Page<ScanGroup> findHistory(@Param("userId") UUID userId, @Param("source") ScanSource source, Pageable pageable);
}