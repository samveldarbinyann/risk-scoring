package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.trongrid.TronContract;
import com.riskscoring.chainingest.client.dto.trongrid.TronContractValue;
import com.riskscoring.chainingest.client.dto.trongrid.TronParameter;
import com.riskscoring.chainingest.client.dto.trongrid.TronRawData;
import com.riskscoring.chainingest.client.dto.trongrid.TronRet;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransaction;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransactionInfo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TronValuesTest {

    private final TronValues values = new TronValues(new TronAddressCodec());

    @Test
    void addressPassesThroughAlreadyBase58Values() {
        assertThat(values.address("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"))
                .isEqualTo("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t");
    }

    @Test
    void addressDelegatesHexValuesToCodec() {
        assertThat(values.address("41a614f803b6fd780986a42c78ec9c7f77e6ded13c"))
                .isEqualTo("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t");
    }

    @Test
    void isRoutableIsFalseOnlyForEmptyAddress() {
        assertThat(values.isRoutable("")).isFalse();
        assertThat(values.isRoutable("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t")).isTrue();
    }

    @Test
    void timestampIsNullForNullOrZeroEpochMillis() {
        assertThat(values.timestamp(null)).isNull();
        assertThat(values.timestamp(0L)).isNull();
        assertThat(values.timestamp(1700000000000L)).isEqualTo(Instant.ofEpochMilli(1700000000000L));
    }

    @Test
    void blockTimeDefaultsToZeroForNullTimestamp() {
        assertThat(values.blockTime(new TronTransactionInfo(null))).isZero();
        assertThat(values.blockTime(new TronTransactionInfo(500L))).isEqualTo(500L);
    }

    @Test
    void amountDefaultsToZeroForNullAmount() {
        assertThat(values.amount(new TronContractValue("owner", "to", null))).isEqualTo(BigInteger.ZERO);
        assertThat(values.amount(new TronContractValue("owner", "to", 100L))).isEqualTo(BigInteger.valueOf(100L));
    }

    @Test
    void succeededIsTrueForNullOrEmptyRetList() {
        assertThat(values.succeeded(transactionWithRet(null))).isTrue();
        assertThat(values.succeeded(transactionWithRet(List.of()))).isTrue();
    }

    @Test
    void succeededRequiresAllContractRetToBeSuccessOrNull() {
        assertThat(values.succeeded(transactionWithRet(List.of(new TronRet("SUCCESS"))))).isTrue();
        assertThat(values.succeeded(transactionWithRet(List.of(new TronRet(null))))).isTrue();
        assertThat(values.succeeded(transactionWithRet(List.of(new TronRet("REVERT"))))).isFalse();
    }

    @Test
    void contractIsEmptyWhenRawDataOrContractListMissing() {
        assertThat(values.contract(transactionWithRawData(null))).isEmpty();
        assertThat(values.contract(transactionWithRawData(new TronRawData(List.of())))).isEmpty();
    }

    @Test
    void contractReturnsFirstElement() {
        TronContract first = new TronContract("TransferContract", null);
        TronContract second = new TronContract("TriggerSmartContract", null);

        assertThat(values.contract(transactionWithRawData(new TronRawData(List.of(first, second)))))
                .contains(first);
    }

    @Test
    void valueIsEmptyWhenParameterMissing() {
        assertThat(values.value(new TronContract("TransferContract", null))).isEmpty();
    }

    @Test
    void valueReturnsParameterValue() {
        TronContractValue contractValue = new TronContractValue("owner", "to", 100L);
        Optional<TronContractValue> result = values.value(new TronContract("TransferContract",
                new TronParameter(contractValue)));

        assertThat(result).contains(contractValue);
    }

    @Test
    void rawAmountIsZeroForBlankOrNonNumericInput() {
        assertThat(values.rawAmount(null)).isEqualTo(BigInteger.ZERO);
        assertThat(values.rawAmount("")).isEqualTo(BigInteger.ZERO);
        assertThat(values.rawAmount("not-a-number")).isEqualTo(BigInteger.ZERO);
    }

    @Test
    void rawAmountParsesValidNumericString() {
        assertThat(values.rawAmount("12345")).isEqualTo(BigInteger.valueOf(12345L));
    }

    @Test
    void scaledMovesDecimalPointLeftByDecimals() {
        assertThat(values.scaled("1000000", 6)).isEqualTo("1.000000");
        assertThat(values.scaled("not-a-number", 6)).isEqualTo(BigDecimal.ZERO.toPlainString());
    }

    private static TronTransaction transactionWithRet(List<TronRet> ret) {
        return new TronTransaction("txid", 0L, ret, null);
    }

    private static TronTransaction transactionWithRawData(TronRawData rawData) {
        return new TronTransaction("txid", 0L, null, rawData);
    }
}
