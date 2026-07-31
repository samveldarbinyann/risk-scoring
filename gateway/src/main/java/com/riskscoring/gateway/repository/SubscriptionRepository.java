package com.riskscoring.gateway.repository;

import com.riskscoring.gateway.entity.Subscription;
import com.riskscoring.gateway.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);

    Optional<Subscription> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Subscription s
            SET s.requestsUsed = s.requestsUsed + :units,
                s.updatedAt = :now
            WHERE s.id = :id
              AND s.status = :status
              AND s.currentPeriodEnd > :now
              AND s.requestsUsed + :units <= s.monthlyRequestLimit
            """)
    int tryCharge(@Param("id") UUID id,
                   @Param("units") int units,
                   @Param("now") Instant now,
                   @Param("status") SubscriptionStatus status);
}
