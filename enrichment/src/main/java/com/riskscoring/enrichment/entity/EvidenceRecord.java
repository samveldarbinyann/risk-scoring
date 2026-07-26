package com.riskscoring.enrichment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "evidence_record")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EvidenceRecord {

    @Id
    private UUID id;

    @Column(name = "scan_id", nullable = false)
    private UUID scanId;

    @Column(nullable = false, length = 42)
    private String address;

    @Column(name = "chain_id", nullable = false)
    private int chainId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EvidenceRecord record)) {
            return false;
        }
        return id != null && id.equals(record.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}