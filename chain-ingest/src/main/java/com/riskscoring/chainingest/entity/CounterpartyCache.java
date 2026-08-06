package com.riskscoring.chainingest.entity;

import com.riskscoring.common.model.TransferDirection;
import jakarta.persistence.*;
import lombok.*;

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
