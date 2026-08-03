package com.riskscoring.chainingest.entity;

import com.riskscoring.common.model.Chain;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "address_cache")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AddressCache {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Chain chain;

    @Column(nullable = false, length = 128)
    private String address;

    @Column(name = "tx_count", nullable = false)
    private long txCount;

    @Column(name = "tx_count_24h", nullable = false)
    private long txCount24h;

    @Column(name = "balance_native", nullable = false, precision = 78)
    private BigInteger balanceNative;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "token_balances", nullable = false, columnDefinition = "jsonb")
    private String tokenBalances;

    @Column(name = "first_seen_at")
    private Instant firstSeenAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "sample_truncated", nullable = false)
    private boolean sampleTruncated;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Builder.Default
    @OneToMany(mappedBy = "addressCache", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CounterpartyCache> counterparties = new ArrayList<>();

    public void replaceCounterparties(List<CounterpartyCache> replacements) {
        counterparties.clear();
        replacements.forEach(counterparty -> counterparty.setAddressCache(this));
        counterparties.addAll(replacements);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AddressCache addressCache)) {
            return false;
        }
        return id != null && id.equals(addressCache.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}