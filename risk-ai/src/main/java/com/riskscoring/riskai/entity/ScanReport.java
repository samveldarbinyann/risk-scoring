package com.riskscoring.riskai.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "scan_report")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScanReport {

    @Id
    private UUID id;

    @Column(name = "scan_id", nullable = false, unique = true)
    private UUID scanId;

    @Column(nullable = false, length = 42)
    private String address;

    @Column(name = "chain_id", nullable = false)
    private int chainId;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false, columnDefinition = "text")
    private String explanation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "decisive_signals", nullable = false, columnDefinition = "jsonb")
    private String decisiveSignals;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "manual_checks", nullable = false, columnDefinition = "jsonb")
    private String manualChecks;

    @Column(name = "balance_wei", nullable = false, columnDefinition = "text")
    private String balanceWei;

    @Column(name = "tx_count", nullable = false)
    private long txCount;

    @Column(name = "tx_count_24h", nullable = false)
    private long txCount24h;

    @Column(name = "sample_truncated", nullable = false)
    private boolean sampleTruncated;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "token_balances", nullable = false, columnDefinition = "jsonb")
    private String tokenBalances;

    @Column(nullable = false, length = 64)
    private String model;

    @Column(name = "prompt_version", nullable = false, length = 16)
    private String promptVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ScanReport report)) {
            return false;
        }
        return id != null && id.equals(report.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
