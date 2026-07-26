package com.riskscoring.gateway.entity;

import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.event.ScanStage;
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
@Table(name = "scan")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Scan {

    @Id
    private UUID id;

    @Column(nullable = false, length = 42)
    private String address;

    @Column(name = "chain_id", nullable = false)
    private int chainId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ScanStage status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ScanSource source;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Scan scan)) {
            return false;
        }
        return id != null && id.equals(scan.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}