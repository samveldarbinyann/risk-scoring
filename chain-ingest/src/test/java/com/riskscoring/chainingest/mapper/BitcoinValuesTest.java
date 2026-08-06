package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.mempool.MempoolStatus;
import com.riskscoring.chainingest.client.dto.mempool.MempoolTransaction;
import com.riskscoring.chainingest.client.dto.mempool.MempoolVin;
import com.riskscoring.chainingest.client.dto.mempool.MempoolVout;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BitcoinValuesTest {

    private final BitcoinValues values = new BitcoinValues();

    @Test
    void addressNormalizesNullToEmptyAndTrimsWhitespace() {
        assertThat(values.address(null)).isEmpty();
        assertThat(values.address("  bc1qxyz  ")).isEqualTo("bc1qxyz");
    }

    @Test
    void isRoutableIsFalseOnlyForEmptyAddress() {
        assertThat(values.isRoutable("")).isFalse();
        assertThat(values.isRoutable("bc1qxyz")).isTrue();
    }

    @Test
    void timestampIsNullWhenStatusOrBlockTimeIsMissing() {
        assertThat(values.timestamp(null)).isNull();
        assertThat(values.timestamp(new MempoolStatus(false, null, null))).isNull();
    }

    @Test
    void timestampParsesEpochSecondsFromStatus() {
        MempoolStatus status = new MempoolStatus(true, 800000L, 1700000000L);

        assertThat(values.timestamp(status)).isEqualTo(Instant.ofEpochSecond(1700000000L));
    }

    @Test
    void inputsAndOutputsAreNullSafe() {
        MempoolTransaction transaction = new MempoolTransaction("tx", 100, null, null, null);

        assertThat(values.inputs(transaction)).isEmpty();
        assertThat(values.outputs(transaction)).isEmpty();
    }

    @Test
    void inputAddressIsEmptyWhenPrevoutMissing() {
        MempoolVin input = new MempoolVin(null, false);

        assertThat(values.inputAddress(input)).isEmpty();
    }

    @Test
    void inputAddressReturnsNormalizedPrevoutAddress() {
        MempoolVin input = new MempoolVin(new MempoolVout(" bc1qsender ", 5000L), false);

        assertThat(values.inputAddress(input)).isEqualTo("bc1qsender");
    }

    @Test
    void inputValueIsZeroWhenPrevoutMissing() {
        MempoolVin input = new MempoolVin(null, false);

        assertThat(values.inputValue(input)).isZero();
    }

    @Test
    void inputValueReturnsPrevoutValue() {
        MempoolVin input = new MempoolVin(new MempoolVout("bc1qsender", 5000L), false);

        assertThat(values.inputValue(input)).isEqualTo(5000L);
    }
}
