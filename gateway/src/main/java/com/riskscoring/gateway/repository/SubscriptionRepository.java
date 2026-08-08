package com.riskscoring.gateway.repository;

import com.riskscoring.gateway.entity.Subscription;
import com.riskscoring.gateway.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);

    Optional<Subscription> findByUserIdAndStatusIn(UUID userId, Collection<SubscriptionStatus> statuses);

    Optional<Subscription> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Subscription> findByStatusAndPaymentAmount(SubscriptionStatus status, BigDecimal paymentAmount);

    boolean existsByStatusAndPaymentAmount(SubscriptionStatus status, BigDecimal paymentAmount);

    List<Subscription> findByStatusAndPaymentExpiresAtBefore(SubscriptionStatus status, Instant threshold);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Subscription s
            SET s.currentPeriodStart = :periodStart,
                s.currentPeriodEnd = :periodEnd,
                s.requestsUsed = 0,
                s.updatedAt = :now
            WHERE s.id = :id
              AND s.status = :status
              AND s.currentPeriodEnd <= :now
            """)
    void rollPeriod(@Param("id") UUID id,
                    @Param("periodStart") Instant periodStart,
                    @Param("periodEnd") Instant periodEnd,
                    @Param("now") Instant now,
                    @Param("status") SubscriptionStatus status);

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
