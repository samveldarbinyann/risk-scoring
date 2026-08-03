package com.riskscoring.chainingest.entity;

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

import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "transaction_cache")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransactionCache {

    @Id
    private UUID id;

    @Column(name = "chain_id", nullable = false)
    private int chainId;

    @Column(name = "tx_hash", nullable = false, length = 66)
    private String txHash;

    @Column(name = "from_address", nullable = false, length = 42)
    private String fromAddress;

    @Column(name = "to_address", nullable = false, length = 42)
    private String toAddress;

    @Column(name = "value_wei", nullable = false, precision = 78)
    private BigInteger valueWei;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "block_timestamp")
    private Instant blockTimestamp;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String parties;

    @Column(name = "internal_transfer_count", nullable = false)
    private int internalTransferCount;

    @Column(name = "erc20_transfer_count", nullable = false)
    private int erc20TransferCount;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TransactionCache cache)) {
            return false;
        }
        return id != null && id.equals(cache.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
