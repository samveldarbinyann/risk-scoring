package com.riskscoring.chainingest.client.dto;

import com.riskscoring.common.model.TransferDirection;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransferSampleTest {

    private static final Instant NOW = Instant.parse("2024-01-02T00:00:00Z");

    @Test
    void sizeReturnsTransferListSize() {
        TransferSample sample = new TransferSample(List.of(transfer(NOW), transfer(NOW)), false);

        assertEquals(2, sample.size());
    }

    @Test
    void txCount24hCountsOnlyTransfersWithinWindow() {
        Instant inside = NOW.minusSeconds(3600);
        Instant outside = NOW.minusSeconds(48 * 3600);
        TransferSample sample = new TransferSample(List.of(transfer(inside), transfer(outside)), false);

        assertEquals(1, sample.txCount24h(NOW));
    }

    @Test
    void txCount24hExcludesTransfersWithNullTimestamp() {
        TransferSample sample = new TransferSample(List.of(transfer(null), transfer(NOW)), false);

        assertEquals(1, sample.txCount24h(NOW));
    }

    @Test
    void lastActivityAtReturnsMaxNonNullTimestamp() {
        Instant earlier = NOW.minusSeconds(1000);
        TransferSample sample = new TransferSample(List.of(transfer(earlier), transfer(NOW)), false);

        assertEquals(NOW, sample.lastActivityAt());
    }

    @Test
    void lastActivityAtIsNullWhenAllTimestampsAreNullOrListIsEmpty() {
        TransferSample allNull = new TransferSample(List.of(transfer(null)), false);
        TransferSample empty = new TransferSample(List.of(), false);

        assertNull(allNull.lastActivityAt());
        assertNull(empty.lastActivityAt());
    }

    private static Transfer transfer(Instant at) {
        return new Transfer("counterparty", TransferDirection.OUT, BigInteger.ONE, at);
    }
}
