package com.riskscoring.gateway.entity;

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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "scan_group")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScanGroup {

    @Id
    private UUID id;

    @Column(nullable = false, length = 42)
    private String address;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ScanGroup scanGroup)) {
            return false;
        }
        return id != null && id.equals(scanGroup.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}