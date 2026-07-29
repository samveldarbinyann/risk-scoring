package com.riskscoring.gateway.repository;

import com.riskscoring.gateway.entity.EmailVerificationCode;
import com.riskscoring.gateway.model.EmailCodePurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {

    Optional<EmailVerificationCode> findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
            UUID userId, EmailCodePurpose purpose);

    @Modifying
    @Query("""
            UPDATE EmailVerificationCode c
               SET c.attempts = c.attempts + 1
             WHERE c.id = :id
               AND c.attempts < :maxAttempts
            """)
    int incrementAttempts(@Param("id") UUID id, @Param("maxAttempts") int maxAttempts);

    @Modifying
    @Query("""
            UPDATE EmailVerificationCode c
               SET c.usedAt = :usedAt
             WHERE c.id = :id
               AND c.usedAt IS NULL
            """)
    int markUsed(@Param("id") UUID id, @Param("usedAt") Instant usedAt);

    @Modifying
    @Query("""
            DELETE FROM EmailVerificationCode c
             WHERE c.expiresAt < :now
                OR (c.usedAt IS NOT NULL AND c.createdAt < :cutoff)
            """)
    int deleteExpiredAndUsed(@Param("now") Instant now, @Param("cutoff") Instant cutoff);
}