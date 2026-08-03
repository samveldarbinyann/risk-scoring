package com.riskscoring.monitor.entity;

import com.riskscoring.common.model.Chain;
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
@Table(name = "alert")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Alert {

    @Id
    private UUID id;

    @Column(name = "watchlist_entry_id", nullable = false)
    private UUID watchlistEntryId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 128)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Chain chain;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_risk_level", nullable = false, length = 16)
    private RiskLevel previousRiskLevel;

    @Column(name = "previous_score", nullable = false)
    private int previousScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_risk_level", nullable = false, length = 16)
    private RiskLevel newRiskLevel;

    @Column(name = "new_score", nullable = false)
    private int newScore;

    @Column(name = "scan_id", nullable = false)
    private UUID scanId;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Alert alert)) {
            return false;
        }
        return id != null && id.equals(alert.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
