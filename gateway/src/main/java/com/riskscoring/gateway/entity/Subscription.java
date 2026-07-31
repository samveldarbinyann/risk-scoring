package com.riskscoring.gateway.entity;

import com.riskscoring.gateway.model.PlanCode;
import com.riskscoring.gateway.model.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "subscription")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Subscription {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_code", nullable = false, length = 16)
    private PlanCode planCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SubscriptionStatus status;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "monthly_request_limit", nullable = false)
    private int monthlyRequestLimit;

    @Column(name = "requests_used", nullable = false)
    private int requestsUsed;

    @Column(name = "current_period_start", nullable = false)
    private Instant currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private Instant currentPeriodEnd;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Subscription subscription)) {
            return false;
        }
        return id != null && id.equals(subscription.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
