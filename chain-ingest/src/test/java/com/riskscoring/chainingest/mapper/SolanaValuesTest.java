package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.helius.HeliusTransaction;
import com.riskscoring.chainingest.client.dto.helius.HeliusTransactionError;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SolanaValuesTest {

    private final SolanaValues values = new SolanaValues();

    @Test
    void addressNormalizesNullToEmptyAndTrims() {
        assertThat(values.address(null)).isEmpty();
        assertThat(values.address("  addr  ")).isEqualTo("addr");
    }

    @Test
    void isRoutableExcludesEmptyAndSystemProgram() {
        assertThat(values.isRoutable("")).isFalse();
        assertThat(values.isRoutable("11111111111111111111111111111111")).isFalse();
        assertThat(values.isRoutable("realAddress")).isTrue();
    }

    @Test
    void timestampIsNullForZeroEpochSeconds() {
        assertThat(values.timestamp(0L)).isNull();
    }

    @Test
    void timestampParsesNonZeroEpochSeconds() {
        assertThat(values.timestamp(1700000000L)).isEqualTo(Instant.ofEpochSecond(1700000000L));
    }

    @Test
    void amountDefaultsToZeroForNull() {
        assertThat(values.amount(null)).isEqualTo(BigDecimal.ZERO);
        assertThat(values.amount(BigDecimal.TEN)).isEqualTo(BigDecimal.TEN);
    }

    @Test
    void succeededIsTrueWhenNoTransactionError() {
        HeliusTransaction ok = transaction(null);
        HeliusTransaction failed = transaction(new HeliusTransactionError("insufficient funds"));

        assertThat(values.succeeded(ok)).isTrue();
        assertThat(values.succeeded(failed)).isFalse();
    }

    @Test
    void nativeAndTokenTransfersAreNullSafe() {
        HeliusTransaction transaction = new HeliusTransaction("sig", 1700000000L, "fee-payer", null, null, null);

        assertThat(values.nativeTransfers(transaction)).isEmpty();
        assertThat(values.tokenTransfers(transaction)).isEmpty();
    }

    private static HeliusTransaction transaction(HeliusTransactionError error) {
        return new HeliusTransaction("sig", 1700000000L, "fee-payer", error, null, null);
    }
}
