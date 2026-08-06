package com.riskscoring.chainingest.entity;

import com.riskscoring.common.model.Chain;
import jakarta.persistence.*;
import lombok.*;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Chain chain;

    @Column(name = "tx_hash", nullable = false, length = 128)
    private String txHash;

    @Column(name = "from_address", length = 128)
    private String fromAddress;

    @Column(name = "to_address", length = 128)
    private String toAddress;

    @Column(name = "value_native", nullable = false, precision = 78)
    private BigInteger valueNative;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "block_timestamp")
    private Instant blockTimestamp;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String parties;

    @Column(name = "nested_transfer_count", nullable = false)
    private int nestedTransferCount;

    @Column(name = "token_transfer_count", nullable = false)
    private int tokenTransferCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "token_transfers", nullable = false, columnDefinition = "jsonb")
    private String tokenTransfers;

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
