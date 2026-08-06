package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.moralis.MoralisTransaction;
import com.riskscoring.chainingest.exception.ChainDataException;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoralisValuesTest {

    private final MoralisValues values = new MoralisValues();

    @Test
    void timestampIsNullForBlankOrNullInput() {
        assertThat(values.timestamp(null)).isNull();
        assertThat(values.timestamp("  ")).isNull();
    }

    @Test
    void timestampParsesValidIsoInstant() {
        assertThat(values.timestamp("2024-01-01T00:00:00Z")).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
    }

    @Test
    void timestampThrowsChainDataExceptionOnMalformedInput() {
        assertThatThrownBy(() -> values.timestamp("not-a-date"))
                .isInstanceOf(ChainDataException.class)
                .hasMessageContaining("not-a-date");
    }

    @Test
    void weiIsZeroForBlankOrNullInput() {
        assertThat(values.wei(null)).isEqualTo(BigInteger.ZERO);
        assertThat(values.wei("")).isEqualTo(BigInteger.ZERO);
    }

    @Test
    void weiParsesValidNumericString() {
        assertThat(values.wei("12345")).isEqualTo(new BigInteger("12345"));
    }

    @Test
    void weiThrowsChainDataExceptionOnNonNumericInput() {
        assertThatThrownBy(() -> values.wei("not-a-number"))
                .isInstanceOf(ChainDataException.class)
                .hasMessageContaining("not-a-number");
    }

    @Test
    void addressLowercasesAndTrims() {
        assertThat(values.address(null)).isEmpty();
        assertThat(values.address(" 0xABC ")).isEqualTo("0xabc");
    }

    @Test
    void isRoutableExcludesEmptyAndZeroAddress() {
        assertThat(values.isRoutable("")).isFalse();
        assertThat(values.isRoutable("0x0000000000000000000000000000000000000000")).isFalse();
        assertThat(values.isRoutable("0xabc")).isTrue();
    }

    @Test
    void succeededIsFalseOnlyForFailedReceiptStatus() {
        MoralisTransaction failed = transaction("0");
        MoralisTransaction succeeded = transaction("1");
        MoralisTransaction unknownStatus = transaction(null);

        assertThat(values.succeeded(failed)).isFalse();
        assertThat(values.succeeded(succeeded)).isTrue();
        assertThat(values.succeeded(unknownStatus)).isTrue();
    }

    private static MoralisTransaction transaction(String receiptStatus) {
        return new MoralisTransaction("hash", "0xfrom", "0xto", "100", "2024-01-01T00:00:00Z",
                receiptStatus, null, null);
    }
}
