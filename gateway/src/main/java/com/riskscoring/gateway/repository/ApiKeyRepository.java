package com.riskscoring.gateway.repository;

import com.riskscoring.gateway.entity.ApiKey;
import com.riskscoring.gateway.model.ApiKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    List<ApiKey> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<ApiKey> findByIdAndUserId(UUID id, UUID userId);

    Optional<ApiKey> findByKeyHashAndStatus(String keyHash, ApiKeyStatus status);

    long countByUserIdAndStatus(UUID userId, ApiKeyStatus status);

    @Modifying
    @Query("""
            UPDATE ApiKey k
            SET k.lastUsedAt = :now
            WHERE k.id = :id
              AND (k.lastUsedAt IS NULL OR k.lastUsedAt < :staleBefore)
            """)
    void touchLastUsedAt(@Param("id") UUID id,
                         @Param("now") Instant now,
                         @Param("staleBefore") Instant staleBefore);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ApiKey k
            SET k.status = :revokedStatus,
                k.revokedAt = :now
            WHERE k.userId = :userId
              AND k.status = :activeStatus
            """)
    int revokeAllActive(@Param("userId") UUID userId,
                        @Param("now") Instant now,
                        @Param("activeStatus") ApiKeyStatus activeStatus,
                        @Param("revokedStatus") ApiKeyStatus revokedStatus);
}
