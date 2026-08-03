package com.riskscoring.chainingest.entity;

import com.riskscoring.common.model.TransferDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "counterparty_cache")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CounterpartyCache {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_cache_id", nullable = false)
    private AddressCache addressCache;

    @Column(nullable = false, length = 128)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private TransferDirection direction;

    @Column(name = "tx_count", nullable = false)
    private long txCount;

    @Column(name = "total_value_native", nullable = false, precision = 78)
    private BigInteger totalValueNative;

    @Column(nullable = false)
    private int hops;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CounterpartyCache counterparty)) {
            return false;
        }
        return id != null && id.equals(counterparty.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
