package com.riskscoring.enrichment.entity;

import com.riskscoring.common.model.LabelCategory;
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
@Table(name = "label")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Label {

    @Id
    private UUID id;

    @Column(name = "chain_id", nullable = false)
    private int chainId;

    @Column(nullable = false, length = 42)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LabelCategory category;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Label label)) {
            return false;
        }
        return id != null && id.equals(label.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}