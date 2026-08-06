package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.tonapi.TonAccountAddress;
import com.riskscoring.chainingest.client.dto.tonapi.TonAction;
import com.riskscoring.chainingest.client.dto.tonapi.TonEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TonValuesTest {

    private final TonValues values = new TonValues();

    @Test
    void addressNormalizesNullToEmptyLowercasesAndTrims() {
        assertThat(values.address(null)).isEmpty();
        assertThat(values.address("  EQABC  ")).isEqualTo("eqabc");
    }

    @Test
    void partyIsEmptyForNullAccount() {
        assertThat(values.party(null)).isEmpty();
        assertThat(values.party(new TonAccountAddress(" EQXYZ "))).isEqualTo("eqxyz");
    }

    @Test
    void isRoutableIsFalseOnlyForEmptyAddress() {
        assertThat(values.isRoutable("")).isFalse();
        assertThat(values.isRoutable("eqabc")).isTrue();
    }

    @Test
    void timestampIsNullForNullEpochSeconds() {
        assertThat(values.timestamp(null)).isNull();
        assertThat(values.timestamp(1700000000L)).isEqualTo(Instant.ofEpochSecond(1700000000L));
    }

    @Test
    void transferActionsFiltersToTonAndJettonTransferTypes() {
        TonAction tonTransfer = action("TonTransfer", "ok");
        TonAction jettonTransfer = action("JettonTransfer", "ok");
        TonAction other = action("NftTransfer", "ok");
        TonEvent event = new TonEvent("evt", 1700000000L, false, List.of(tonTransfer, jettonTransfer, other));

        assertThat(values.transferActions(event)).containsExactly(tonTransfer, jettonTransfer);
    }

    @Test
    void succeededActionRequiresOkStatus() {
        assertThat(values.succeeded(action("TonTransfer", "ok"))).isTrue();
        assertThat(values.succeeded(action("TonTransfer", "failed"))).isFalse();
    }

    @Test
    void succeededEventRequiresAllActionsToSucceed() {
        TonEvent allOk = new TonEvent("evt", 1L, false, List.of(action("TonTransfer", "ok"), action("TonTransfer", "ok")));
        TonEvent oneFailed = new TonEvent("evt", 1L, false, List.of(action("TonTransfer", "ok"), action("TonTransfer", "failed")));
        TonEvent noActions = new TonEvent("evt", 1L, false, null);

        assertThat(values.succeeded(allOk)).isTrue();
        assertThat(values.succeeded(oneFailed)).isFalse();
        assertThat(values.succeeded(noActions)).isTrue();
    }

    @Test
    void amountDefaultsToZeroForNull() {
        assertThat(values.amount(null)).isEqualTo(BigInteger.ZERO);
        assertThat(values.amount(100L)).isEqualTo(BigInteger.valueOf(100L));
    }

    @Test
    void scaledAmountIsZeroForBlankOrNonNumericRawAmount() {
        assertThat(values.scaledAmount(null, 9)).isEqualTo(BigDecimal.ZERO);
        assertThat(values.scaledAmount("", 9)).isEqualTo(BigDecimal.ZERO);
        assertThat(values.scaledAmount("not-a-number", 9)).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void scaledAmountMovesDecimalPointLeftByDecimals() {
        assertThat(values.scaled("1000000000", 9)).isEqualTo("1.000000000");
    }

    private static TonAction action(String type, String status) {
        return new TonAction(type, status, null, null);
    }
}
