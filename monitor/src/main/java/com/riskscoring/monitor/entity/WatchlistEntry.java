package com.riskscoring.monitor.entity;

import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.RiskLevel;
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
@Table(name = "watchlist_entry")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WatchlistEntry {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 42)
    private String address;

    @Column(name = "chain_id", nullable = false)
    private int chainId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Language language;

    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_risk_level", length = 16)
    private RiskLevel lastRiskLevel;

    @Column(name = "last_score")
    private Integer lastScore;

    @Column(name = "last_scan_id")
    private UUID lastScanId;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "pending_scan_id")
    private UUID pendingScanId;

    @Column(name = "pending_requested_at")
    private Instant pendingRequestedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WatchlistEntry entry)) {
            return false;
        }
        return id != null && id.equals(entry.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
