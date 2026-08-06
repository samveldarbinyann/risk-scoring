package com.riskscoring.enrichment.entity;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ScanTarget;
import jakarta.persistence.*;
import lombok.*;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 16)
    private ScanTarget targetType;

    @Column(nullable = false, length = 128)
    private String target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Chain chain;

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